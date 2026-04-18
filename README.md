# Sharding을 통한 UPDATE 경합 완화

## 1. 문제 배경

부하테스트 연습을 위해 테스트용 쿠폰 발행 서비스를 구현했습니다. 서비스는 유저, 이벤트, 쿠폰으로 구성되어 있고, 다음의 규칙을 가집니다.

### 1-1. 서비스 규칙

1. 유저는 하나의 이벤트당 하나의 쿠폰만 발급받을 수 있다.
2. 이벤트별 총 쿠폰 발행 수량이 정해져 있다.
3. 위 규칙을 위반하면 에러를 반환한다.

### 1-2. 문제 상황

초기 쿠폰 발급 로직은 다음과 같았습니다. 유저와 이벤트를 조회한 뒤, 이벤트 재고를 차감하고 쿠폰을 저장하는 구조입니다.

```java
// get user
UserEntity user = userRepository.findById(request.userId())
    .orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
// get event
EventEntity event = eventRepository.findById(request.eventId())
		.orElseThrow(()-> new CustomException(ErrorCode.EVENT_NOT_FOUND));

// discount amounts
int updated = eventRepository.discountAmounts(request.eventId());
if (updated==0) {
		throw new CustomException(ErrorCode.EVENT_COUPON_EXHAUSTED);
}

// save coupon
try {
    couponRepository.saveAndFlush(CouponEntity.create(user, event));
} catch (DataIntegrityViolationException e) {
		throw new CustomException(ErrorCode.COUPON_DUPLICATED);
}
```

이후 하나의 이벤트에 대해 다수 사용자가 동시에 쿠폰을 발급받는 상황을 가정해 부하 테스트를 진행했습니다. 테스트 과정에서 응답 시간이 급격히 증가했고, 평균 처리 시간이 수 초 단위까지 치솟는 현상을 확인했습니다.

## 2. 원인 분석

### 2-1. 조회 비용이 원인인지 확인

처음에는 조회 쿼리 수가 많아 응답이 느려졌다고 가정했습니다.
유저 조회, 이벤트 조회, 재고 차감 UPDATE, 쿠폰 저장까지 여러 차례 DB와 왕복하는 구조였기 때문에, 우선 조회 비용 절감 가능성을 검토했습니다.

이 과정에서 `findById` 대신 `getReferenceById`를 적용해 조회 비용을 줄일 수 있는지 확인했습니다. 그러나 일부 조회 비용은 줄일 수 있었지만 전체 처리 시간은 크게 개선되지 않았습니다.

```java
// get user
UserEntity user = userRepository.findById(request.userId())
    .orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));

// 다음과 같은 방식으로 변경
UserEntity user = userRepository.getReferenceById(request.userId())
```

### 2-2. 애플리케이션 자원 문제 여부 확인

다음으로 커넥션 풀 크기와 DB Connection 수를 충분히 늘려 애플리케이션 자원 부족 문제를 의심해봤습니다.
또한 CPU 사용량도 함께 확인했지만 이 역시 병목의 직접적인 원인은 아니었습니다.

### 2-3. 단일 이벤트 row에 대한 UPDATE 경합

각 구간을 분리해서 확인한 결과, 병목은 `UPDATE` 구간에서 발생하고 있었습니다. 모든 요청이 동일한 이벤트의 재고를 차감하기 위해 하나의 row에 집중되면서 DB 레벨에서 경합이 발생한 것입니다.

JUnit Test를 진행할 땐 문제가 없어서 넘어갔었는데 원인을 확인해보니 테스트 환경이 H2 인메모리 DB를 사용했기 때문에 Lock 경합 특성이 충분히 재현되지 않아 문제가 없는 것처럼 보인것이었습니다. . 이에 MySQL로 바꿔 테스트를 진행해보니 같은 지점에서 병목이 발생하는 것을 확인했습니다.

문제 원인은 하나의 이벤트 ROW에 집중되는 UPDATE 경합이었습니다.

## 3. 해결 방향

요청들이 하나의 이벤트에 접근하여 문제가 발생하고 있었기 때문에 이벤트 재고를 하나의 row로 관리하는 대신, 여러 개의 `EventShard` row로 분산 저장하는 구조를 도입했습니다. 사용자는 이벤트 하나에 신청한다는 도메인 규칙을 따르면서도, 내부적으로는 shard 단위 재고를 차감해 경합을 분산시켰습니다.

사용자에게 보이는 도메인은 이벤트 단위, 내부 재고 관리는 샤드 단위, 중복 발급 제약은 유저와 이벤트를 기준으로 설계했습니다.

## 4. Sharding 적용 방식

요청되는 User ID는 랜덤으로 온다 가정하여 첫 샤드 접근은 `userId % shardCount`로 결정했습니다. 이후 선택 샤드에 재고가 없으면 다른 shard를 순차적으로 탐색하도록 구성했습니다. 이를 통해 특정 shard가 먼저 소진되더라도 이벤트 전체 재고가 남아 있다면 발급이 가능하도록 했습니다.

```java
@Transactional
public CouponIssueResponse issue(CouponIssueRequest request) {
    // get user
    UserEntity user = userRepository.findById(request.userId())
            .orElseThrow(()-> new CustomException(ErrorCode.USER_NOT_FOUND));
    // get event
    EventEntity event = eventRepository.findById(request.eventId())
            .orElseThrow(()-> new CustomException(ErrorCode.EVENT_NOT_FOUND));

    // discount amounts
    int shardCount = request.shardCount();
    int updated = 0;
    int shardNo = 0;
    for (int idx=0;idx<shardCount;idx++) {
        shardNo = (user.getId()+idx)%shardCount;
        updated = eventShardRepository.discountAmounts(event,shardNo);
        if (updated > 0) {
            break;
        }
    }
    if (updated==0) {
        throw new CustomException(ErrorCode.EVENT_COUPON_EXHAUSTED);
    }

    // save coupon
    try {
        couponRepository.saveAndFlush(CouponEntity.create(user, event, shardNo));
    } catch (DataIntegrityViolationException e) {
        throw new CustomException(ErrorCode.COUPON_DUPLICATED);
    }

    return new CouponIssueResponse(true);
}
```

## 5. 결과

샤딩 적용 전후 성능을 비교한 결과, shard 수가 증가할수록 평균 응답 시간과 p95 지표가 크게 개선되는 것을 확인했습니다. 특히 shard 수가 10 이상으로 늘어나자 평균 처리 시간이 초 단위에서 밀리초 단위로 크게 감소했습니다.

| 초당 300건/ 30초 | 요청 성공 | DB 에러 | iter 실패 | 평균 처리 시간 | 최장 처리 시간 | p(95) |
| --- | --- | --- | --- | --- | --- | --- |
| 샤딩 없음 | 6568 | 37 | 2395 | 30.84 S | 58.5 S | 55.8 S |
| 3 | 7755 | 0 | 1245 | 11.53 S | 19.68 S | 19.23 S |
| 5 | 9000 | 0 | 0 | 3.71 S | 7 S | 5.69 S |
| 10 | 9000 | 0 | 0 | 44.71 MS | 1.02 S | 215.15 MS |
| 50 | 9000 | 0 | 0 | 29.56 MS | 376.04 MS | 67.26 MS |

## 6. 한계와 고려사항

### 6-1. fallback 순회 편향 가능성

현재 구조는 1차 선택 shard에 재고가 없으면 다른 shard를 순차적으로 탐색하는 방식입니다.
이 방식은 구현이 단순하고 남은 재고를 활용하기 쉽다는 장점이 있지만, fallback 순서가 고정되어 있어 특정 shard에 탐색이 편향될 수 있습니다.

이는 순차 탐색 대신 사용자별 해시값을 활용해 순회 시작점이나 순회 간격을 다르게 주는 방식을 고려해 볼 수 있습니다.

### 6-2. 추가 제어를 통한 성능 향상

성능 향상을 위해 다음의 방식을 도입해 볼 수 있습니다.

- 이미 소진된 shard는 조회하지 않도록 Redis 기반 사전 제어
- 쿠폰 저장과 같은 후속 처리는 Kafka 기반 비동기 처리로 분리
    - 다만 비동기 분산 처리는 재고 차감과 쿠폰 저장 간 정합성 보장, 중복 발급 방지, 실패 시 재처리 전략까지 함께 고려해 설계해야 합니다.