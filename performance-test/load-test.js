import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },  // ウォームアップ
    { duration: '1m', target: 50 },   // 50同時ユーザー
    { duration: '2m', target: 100 },  // 100同時ユーザー
    { duration: '30s', target: 0 },   // クールダウン
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // p95が500ms以内
    http_req_failed: ['rate<0.01'],    // エラー率1%未満
  },
};

export default function () {
  // 商品一覧取得
  let res = http.get('http://localhost:3001/api/products');
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(1);

  // 商品詳細取得
  res = http.get('http://localhost:3001/api/products/1');
  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
