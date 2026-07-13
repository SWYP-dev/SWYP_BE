import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '30s', target: 30 },
    { duration: '30s', target: 50 },
  ],
};

// 조합 필터: keyword + region(서울) + jobCategory(정보통신) + deadlineSoon + sort=DEADLINE
const QUERY = 'keyword=Load'
  + '&region=%EC%84%9C%EC%9A%B8'
  + '&jobCategory=%EC%A0%95%EB%B3%B4%ED%86%B5%EC%8B%A0'
  + '&deadlineSoon=true'
  + '&sort=DEADLINE';

const BASE_URL = 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/feed?${QUERY}`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(0.5);
}
