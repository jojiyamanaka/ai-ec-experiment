#!/bin/bash

# H-3 注文状態遷移機能のテストスクリプト

set -e

echo "=========================================="
echo "H-3 注文状態遷移機能 テスト"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8080"
SESSION_ID_1="test-session-$(date +%s)-1"
SESSION_ID_2="test-session-$(date +%s)-2"
SESSION_ID_3="test-session-$(date +%s)-3"

echo "📋 テスト環境:"
echo "  - BASE_URL: $BASE_URL"
echo "  - SESSION_ID_1: $SESSION_ID_1"
echo "  - SESSION_ID_2: $SESSION_ID_2"
echo "  - SESSION_ID_3: $SESSION_ID_3"
echo ""

# ヘルパー関数
check_status() {
    if [ $? -eq 0 ]; then
        echo "✅ PASS"
    else
        echo "❌ FAIL"
        exit 1
    fi
}

echo "=========================================="
echo "テスト 1: 注文作成（PENDING）"
echo "=========================================="
echo "カートに商品追加..."
curl -s -X POST "$BASE_URL/api/order/cart/items" \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: $SESSION_ID_1" \
  -d '{"productId": 1, "quantity": 2}' | jq .
check_status

echo ""
echo "注文作成..."
ORDER_1=$(curl -s -X POST "$BASE_URL/api/order" \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: $SESSION_ID_1" \
  -d "{\"cartId\": \"$SESSION_ID_1\"}")
echo "$ORDER_1" | jq .
ORDER_ID_1=$(echo "$ORDER_1" | jq -r '.data.orderId')
ORDER_STATUS=$(echo "$ORDER_1" | jq -r '.data.status')

if [ "$ORDER_STATUS" = "PENDING" ]; then
    echo "✅ 注文作成成功（orderId: $ORDER_ID_1, status: PENDING）"
else
    echo "❌ 注文作成失敗（status: $ORDER_STATUS）"
    exit 1
fi
echo ""

echo "=========================================="
echo "テスト 2: 注文確認（PENDING → CONFIRMED）"
echo "=========================================="
RESULT=$(curl -s -X POST "$BASE_URL/api/order/$ORDER_ID_1/confirm")
echo "$RESULT" | jq .
STATUS=$(echo "$RESULT" | jq -r '.data.status')

if [ "$STATUS" = "CONFIRMED" ]; then
    echo "✅ 注文確認成功（status: CONFIRMED）"
else
    echo "❌ 注文確認失敗（status: $STATUS）"
    exit 1
fi
echo ""

echo "=========================================="
echo "テスト 3: 注文発送（CONFIRMED → SHIPPED）"
echo "=========================================="
RESULT=$(curl -s -X POST "$BASE_URL/api/order/$ORDER_ID_1/ship")
echo "$RESULT" | jq .
STATUS=$(echo "$RESULT" | jq -r '.data.status')

if [ "$STATUS" = "SHIPPED" ]; then
    echo "✅ 注文発送成功（status: SHIPPED）"
else
    echo "❌ 注文発送失敗（status: $STATUS）"
    exit 1
fi
echo ""

echo "=========================================="
echo "テスト 4: 配達完了（SHIPPED → DELIVERED）"
echo "=========================================="
RESULT=$(curl -s -X POST "$BASE_URL/api/order/$ORDER_ID_1/deliver")
echo "$RESULT" | jq .
STATUS=$(echo "$RESULT" | jq -r '.data.status')

if [ "$STATUS" = "DELIVERED" ]; then
    echo "✅ 配達完了成功（status: DELIVERED）"
else
    echo "❌ 配達完了失敗（status: $STATUS）"
    exit 1
fi
echo ""

echo "=========================================="
echo "テスト 5: 注文キャンセル（PENDING → CANCELLED）"
echo "=========================================="
echo "新規注文作成..."
curl -s -X POST "$BASE_URL/api/order/cart/items" \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: $SESSION_ID_2" \
  -d '{"productId": 2, "quantity": 1}' > /dev/null

ORDER_2=$(curl -s -X POST "$BASE_URL/api/order" \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: $SESSION_ID_2" \
  -d "{\"cartId\": \"$SESSION_ID_2\"}")
ORDER_ID_2=$(echo "$ORDER_2" | jq -r '.data.orderId')
echo "注文ID: $ORDER_ID_2"

echo ""
echo "注文キャンセル..."
RESULT=$(curl -s -X POST "$BASE_URL/api/order/$ORDER_ID_2/cancel" \
  -H "X-Session-Id: $SESSION_ID_2")
echo "$RESULT" | jq .
STATUS=$(echo "$RESULT" | jq -r '.data.status')

if [ "$STATUS" = "CANCELLED" ]; then
    echo "✅ 注文キャンセル成功（status: CANCELLED）"
else
    echo "❌ 注文キャンセル失敗（status: $STATUS）"
    exit 1
fi
echo ""

echo "=========================================="
echo "テスト 6: 全注文取得"
echo "=========================================="
RESULT=$(curl -s "$BASE_URL/api/order")
echo "$RESULT" | jq '.data | length'
ORDER_COUNT=$(echo "$RESULT" | jq '.data | length')

if [ "$ORDER_COUNT" -ge 2 ]; then
    echo "✅ 全注文取得成功（件数: $ORDER_COUNT）"
else
    echo "❌ 全注文取得失敗（件数: $ORDER_COUNT）"
    exit 1
fi
echo ""

echo "=========================================="
echo "テスト 7: エラーケース - 不正な状態遷移"
echo "=========================================="
echo "新規注文作成（PENDING）..."
curl -s -X POST "$BASE_URL/api/order/cart/items" \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: $SESSION_ID_3" \
  -d '{"productId": 1, "quantity": 1}' > /dev/null

ORDER_3=$(curl -s -X POST "$BASE_URL/api/order" \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: $SESSION_ID_3" \
  -d "{\"cartId\": \"$SESSION_ID_3\"}")
ORDER_ID_3=$(echo "$ORDER_3" | jq -r '.data.orderId')
echo "注文ID: $ORDER_ID_3"

echo ""
echo "PENDING から直接 SHIPPED へ遷移試行..."
RESULT=$(curl -s -X POST "$BASE_URL/api/order/$ORDER_ID_3/ship")
echo "$RESULT" | jq .
ERROR_CODE=$(echo "$RESULT" | jq -r '.error.code')

if [ "$ERROR_CODE" = "INVALID_STATUS_TRANSITION" ]; then
    echo "✅ 不正な状態遷移を正しく拒否（ERROR: INVALID_STATUS_TRANSITION）"
else
    echo "❌ 不正な状態遷移の拒否に失敗（ERROR: $ERROR_CODE）"
    exit 1
fi
echo ""

echo "=========================================="
echo "テスト 8: エラーケース - DELIVERED でキャンセル試行"
echo "=========================================="
RESULT=$(curl -s -X POST "$BASE_URL/api/order/$ORDER_ID_1/cancel" \
  -H "X-Session-Id: $SESSION_ID_1")
echo "$RESULT" | jq .
ERROR_CODE=$(echo "$RESULT" | jq -r '.error.code')

if [ "$ERROR_CODE" = "ORDER_NOT_CANCELLABLE" ]; then
    echo "✅ DELIVERED 状態のキャンセルを正しく拒否（ERROR: ORDER_NOT_CANCELLABLE）"
else
    echo "❌ DELIVERED 状態のキャンセル拒否に失敗（ERROR: $ERROR_CODE）"
    exit 1
fi
echo ""

echo "=========================================="
echo "テスト 9: エラーケース - 既にキャンセル済みをキャンセル試行"
echo "=========================================="
RESULT=$(curl -s -X POST "$BASE_URL/api/order/$ORDER_ID_2/cancel" \
  -H "X-Session-Id: $SESSION_ID_2")
echo "$RESULT" | jq .
ERROR_CODE=$(echo "$RESULT" | jq -r '.error.code')

if [ "$ERROR_CODE" = "ALREADY_CANCELLED" ]; then
    echo "✅ 既にキャンセル済みの注文キャンセルを正しく拒否（ERROR: ALREADY_CANCELLED）"
else
    echo "❌ 既にキャンセル済みの注文キャンセル拒否に失敗（ERROR: $ERROR_CODE）"
    exit 1
fi
echo ""

echo "=========================================="
echo "✅ 全テスト完了"
echo "=========================================="
echo ""
echo "テスト結果サマリー:"
echo "  1. 注文作成（PENDING）: ✅"
echo "  2. 注文確認（PENDING → CONFIRMED）: ✅"
echo "  3. 注文発送（CONFIRMED → SHIPPED）: ✅"
echo "  4. 配達完了（SHIPPED → DELIVERED）: ✅"
echo "  5. 注文キャンセル（PENDING → CANCELLED）: ✅"
echo "  6. 全注文取得: ✅"
echo "  7. 不正な状態遷移の拒否: ✅"
echo "  8. DELIVERED でキャンセル拒否: ✅"
echo "  9. 既にキャンセル済みをキャンセル拒否: ✅"
echo ""
echo "🎉 すべてのテストが成功しました！"
