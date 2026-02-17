import http from 'k6/http';
import { check, sleep } from 'k6';

const TOKEN = __ENV.USER_TOKEN;  // 環境変数から取得

export const options = {
  stages: [
    { duration: '1m', target: 50 },
    { duration: '2m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],  // 認証ありは800ms以内
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const params = {
    headers: {
      'Authorization': `Bearer ${TOKEN}`,
    },
  };

  // カート取得
  let res = http.get('http://localhost:3001/api/cart', params);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(2);

  // 注文履歴
  res = http.get('http://localhost:3001/api/orders', params);
  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(2);
}
