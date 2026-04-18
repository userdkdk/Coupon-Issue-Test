import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const EVENT_ID = Number(__ENV.EVENT_ID || 1);
const SHARD_COUNT = Number(__ENV.SHARD_COUNT || 1);

const duplicateCounts = new Counter('duplicate_counts');
const successIssues = new Counter('success_issues');
const serverErrors = new Counter('server_errors');
const soldOutRejects = new Counter('sold_out_rejects');
const duplicateSuccessRate = new Rate('duplicate_success_rate');
const notFoundErrors = new Counter('not_found_errors');
const unexpectedDuplicateRejects = new Counter('unexpected_duplicate_rejects');

const DUPLICATE_USER_POOL_SIZE = Number(__ENV.DUPLICATE_USER_POOL_SIZE || 1000);
const DUPLICATE_USERS = Array.from({ length: DUPLICATE_USER_POOL_SIZE }, (_, i) => i + 1);

const PRESETS = {
  smoke: {
    rate: 10,
    timeUnit: '1s',
    duration: '10s',
    preAllocatedVUs: 50,
    maxVUs: 200,
    gracefulStop: '30s',
    timeOut: '30s',
  },
  normal: {
    rate: 100,
    timeUnit: '1s',
    duration: '30s',
    preAllocatedVUs: 2000,
    maxVUs: 6000,
    gracefulStop: '60s',
    timeOut: '60s',
  },
  spike: {
    rate: 300,
    timeUnit: '1s',
    duration: '30s',
    preAllocatedVUs: 2000,
    maxVUs: 6000,
    gracefulStop: '90s',
    timeOut: '90s',
  },
  soak: {
    rate: 50,
    timeUnit: '1s',
    duration: '10m',
    preAllocatedVUs: 1000,
    maxVUs: 3000,
    gracefulStop: '120s',
    timeOut: '120s',
  },
};

const presetName = __ENV.PRESET || 'normal';
const selectedPreset = PRESETS[presetName];

if (!selectedPreset) {
  throw new Error(
    `Unknown PRESET: ${presetName}. Available presets: ${Object.keys(PRESETS).join(', ')}`
  );
}

const rate = Number(__ENV.RATE || selectedPreset.rate);
const timeUnit = __ENV.TIME_UNIT || selectedPreset.timeUnit;
const duration = __ENV.DURATION || selectedPreset.duration;
const preAllocatedVUs = Number(__ENV.PRE_ALLOCATED_VUS || selectedPreset.preAllocatedVUs);
const maxVUs = Number(__ENV.MAX_VUS || selectedPreset.maxVUs);
const gracefulStop = __ENV.GRACEFUL_STOP || selectedPreset.gracefulStop;
const requestTimeout = __ENV.REQUEST_TIMEOUT || selectedPreset.timeout;

const duplicateRatio = Number(__ENV.DUPLICATE_RATIO || 0);
if (duplicateRatio < 0 || duplicateRatio > 1) {
  throw new Error(`DUPLICATE_RATIO must be between 0 and 1. current=${duplicateRatio}`);
}

export const options = {
  scenarios: {
    coupon_open_burst: {
      executor: 'constant-arrival-rate',
      rate,
      timeUnit,
      duration,
      preAllocatedVUs,
      maxVUs,
      gracefulStop,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    dropped_iterations: ['count<100'],
    duplicate_success_rate: ['rate<0.001'],
  },
};

function nextUniqueUserId() {
  return DUPLICATE_USER_POOL_SIZE + exec.scenario.iterationInTest + 40000;
}

function pickDuplicateUserId() {
  const idx = Math.floor(Math.random() * DUPLICATE_USERS.length);
  return DUPLICATE_USERS[idx];
}

export default function () {
  // const isDuplicate = Math.random() < duplicateRatio;
  const isDuplicate = false;
  const userId = isDuplicate ? pickDuplicateUserId() : nextUniqueUserId();

  const payload = JSON.stringify({
    userId,
    eventId: EVENT_ID,
    shardCount: SHARD_COUNT,
  });

  const res = http.post(`${BASE_URL}/api/v1/coupon`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (res.status === 201 || res.status === 200) {
    successIssues.add(1);

    if (isDuplicate) {
      duplicateSuccessRate.add(0, { request_type: 'duplicate' });
    }
  } else if (res.status === 400) {
    soldOutRejects.add(1);
  } else if (res.status === 409) {
    if (isDuplicate) {
      duplicateCounts.add(1, { request_type: 'duplicate' });
      duplicateSuccessRate.add(1, { request_type: 'duplicate' });
    } else {
      unexpectedDuplicateRejects.add(1);
    }
  } else if (res.status === 404) {
    notFoundErrors.add(1);
  } else {
    serverErrors.add(1);
  }

  if (res.status >= 500) {
    console.log(
      `status=${res.status}, body=${res.body}, userId=${userId}, eventId=${EVENT_ID}, preset=${presetName}`
    );
  }

  check(res, {
    'not server failure': (r) => r.status < 500,
  });
}