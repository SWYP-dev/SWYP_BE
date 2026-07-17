import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '30s', target: 30 },
    { duration: '30s', target: 50 },
  ],
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/feed`);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(0.5);
}
