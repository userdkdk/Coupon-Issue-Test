import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Rate} from 'k6/metrics';

const BASE_URL = 'http://127.0.0.1:8080';

const successIssues = new Counter('success_issues');
const serverErrors = new Counter('server_errors');
const notFoundErrors = new Counter('not_found_errors'); // 404 에러 카운트
const USER_POOL_SIZE = 60005;

export const options = {
  scenarios: {
    coupon_open_burst: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 2000,
      maxVUs: 6000,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    dropped_iterations: ['count<100'],

  },
};

function nextUniqueUserId() {
  return (USER_POOL_SIZE + exec.scenario.iterationInTest + 1);
}

function nextUniqueName() {
  return "event" + (USER_POOL_SIZE + exec.scenario.iterationInTest + 1);
}

export default function () {
  // const userId = nextUniqueUserId();
  const eventName = nextUniqueName();

  const payload = JSON.stringify({
    name: eventName,
  });

  const res = http.post(`${BASE_URL}/api/v1/events`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (res.status === 201 || res.status === 200) {
    successIssues.add(1);
  }else if (res.status === 404) {
    // not found인 경우
    notFoundErrors.add(1);
  } else {
    serverErrors.add(1);
  }

  check(res, {
    'not server failure': (r) => r.status < 500,
  });
}