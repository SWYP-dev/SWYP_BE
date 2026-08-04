import http from 'k6/http';
import { check, sleep } from 'k6';

// 배포 서버(api.chwihap.com) 대상 부하테스트. 실사용자 트래픽 보호를 위해
// 실패율 5% 초과 또는 p95 응답시간 5초 초과 시 즉시 중단(abortOnFail)한다.
export const options = {
  stages: [
    { duration: '20s', target: 50 },
    { duration: '20s', target: 100 },
    { duration: '20s', target: 150 },
    { duration: '30s', target: 200 },
  ],
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: true, delayAbortEval: '5s' }],
    http_req_duration: [{ threshold: 'p(95)<5000', abortOnFail: true, delayAbortEval: '5s' }],
  },
};

const BASE_URL = 'https://api.chwihap.com';

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/feed`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(0.5);
}
