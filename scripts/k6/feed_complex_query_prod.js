import http from 'k6/http';
import { check, sleep } from 'k6';

// 배포 서버(api.chwihap.com) 대상 부하테스트. 실사용자 트래픽 보호를 위해
// 실패율 5% 초과 또는 p95 응답시간 5초 초과 시 즉시 중단(abortOnFail)한다.
export const options = {
  stages: [
    { duration: '20s', target: 50 },
    { duration: '20s', target: 100 },
    { duration: '30s', target: 150 },
  ],
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: true, delayAbortEval: '5s' }],
    http_req_duration: [{ threshold: 'p(95)<5000', abortOnFail: true, delayAbortEval: '5s' }],
  },
};

// 조합 필터: keyword + region(서울) + jobCategory(정보통신) + deadlineSoon + sort=DEADLINE
const QUERY = 'keyword=Load'
  + '&region=%EC%84%9C%EC%9A%B8'
  + '&jobCategory=%EC%A0%95%EB%B3%B4%ED%86%B5%EC%8B%A0'
  + '&deadlineSoon=true'
  + '&sort=DEADLINE';

const BASE_URL = 'https://api.chwihap.com';

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/feed?${QUERY}`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(0.5);
}
