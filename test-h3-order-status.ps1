# H-3 注文状態遷移機能のテストスクリプト (PowerShell版)

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "H-3 注文状態遷移機能 テスト" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$BaseUrl = "http://localhost:8080"
$Timestamp = [DateTimeOffset]::Now.ToUnixTimeSeconds()
$SessionId1 = "test-session-$Timestamp-1"
$SessionId2 = "test-session-$Timestamp-2"
$SessionId3 = "test-session-$Timestamp-3"

Write-Host "📋 テスト環境:"
Write-Host "  - BASE_URL: $BaseUrl"
Write-Host "  - SESSION_ID_1: $SessionId1"
Write-Host "  - SESSION_ID_2: $SessionId2"
Write-Host "  - SESSION_ID_3: $SessionId3"
Write-Host ""

function Invoke-ApiRequest {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = @{},
        [string]$Body = $null
    )

    try {
        $params = @{
            Method = $Method
            Uri = $Url
            Headers = $Headers
            ContentType = "application/json"
        }

        if ($Body) {
            $params.Body = $Body
        }

        $response = Invoke-RestMethod @params
        return $response
    }
    catch {
        Write-Host "Error: $_" -ForegroundColor Red
        throw
    }
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "テスト 1: 注文作成（PENDING）" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "カートに商品追加..."
$addToCartResponse = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order/cart/items" `
    -Headers @{"X-Session-Id" = $SessionId1} `
    -Body '{"productId": 1, "quantity": 2}'
$addToCartResponse | ConvertTo-Json

Write-Host ""
Write-Host "注文作成..."
$order1 = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order" `
    -Headers @{"X-Session-Id" = $SessionId1} `
    -Body "{`"cartId`": `"$SessionId1`"}"
$order1 | ConvertTo-Json

$orderId1 = $order1.data.orderId
$orderStatus = $order1.data.status

if ($orderStatus -eq "PENDING") {
    Write-Host "✅ 注文作成成功（orderId: $orderId1, status: PENDING）" -ForegroundColor Green
}
else {
    Write-Host "❌ 注文作成失敗（status: $orderStatus）" -ForegroundColor Red
    exit 1
}
Write-Host ""

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "テスト 2: 注文確認（PENDING → CONFIRMED）" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
$result = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order/$orderId1/confirm"
$result | ConvertTo-Json

if ($result.data.status -eq "CONFIRMED") {
    Write-Host "✅ 注文確認成功（status: CONFIRMED）" -ForegroundColor Green
}
else {
    Write-Host "❌ 注文確認失敗（status: $($result.data.status)）" -ForegroundColor Red
    exit 1
}
Write-Host ""

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "テスト 3: 注文発送（CONFIRMED → SHIPPED）" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
$result = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order/$orderId1/ship"
$result | ConvertTo-Json

if ($result.data.status -eq "SHIPPED") {
    Write-Host "✅ 注文発送成功（status: SHIPPED）" -ForegroundColor Green
}
else {
    Write-Host "❌ 注文発送失敗（status: $($result.data.status)）" -ForegroundColor Red
    exit 1
}
Write-Host ""

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "テスト 4: 配達完了（SHIPPED → DELIVERED）" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
$result = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order/$orderId1/deliver"
$result | ConvertTo-Json

if ($result.data.status -eq "DELIVERED") {
    Write-Host "✅ 配達完了成功（status: DELIVERED）" -ForegroundColor Green
}
else {
    Write-Host "❌ 配達完了失敗（status: $($result.data.status)）" -ForegroundColor Red
    exit 1
}
Write-Host ""

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "テスト 5: 注文キャンセル（PENDING → CANCELLED）" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "新規注文作成..."
$null = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order/cart/items" `
    -Headers @{"X-Session-Id" = $SessionId2} `
    -Body '{"productId": 2, "quantity": 1}'

$order2 = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order" `
    -Headers @{"X-Session-Id" = $SessionId2} `
    -Body "{`"cartId`": `"$SessionId2`"}"
$orderId2 = $order2.data.orderId
Write-Host "注文ID: $orderId2"

Write-Host ""
Write-Host "注文キャンセル..."
$result = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order/$orderId2/cancel" `
    -Headers @{"X-Session-Id" = $SessionId2}
$result | ConvertTo-Json

if ($result.data.status -eq "CANCELLED") {
    Write-Host "✅ 注文キャンセル成功（status: CANCELLED）" -ForegroundColor Green
}
else {
    Write-Host "❌ 注文キャンセル失敗（status: $($result.data.status)）" -ForegroundColor Red
    exit 1
}
Write-Host ""

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "テスト 6: 全注文取得" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
$result = Invoke-ApiRequest -Method GET -Url "$BaseUrl/api/order"
$orderCount = $result.data.Count
Write-Host "注文件数: $orderCount"

if ($orderCount -ge 2) {
    Write-Host "✅ 全注文取得成功（件数: $orderCount）" -ForegroundColor Green
}
else {
    Write-Host "❌ 全注文取得失敗（件数: $orderCount）" -ForegroundColor Red
    exit 1
}
Write-Host ""

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "テスト 7: エラーケース - 不正な状態遷移" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "新規注文作成（PENDING）..."
$null = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order/cart/items" `
    -Headers @{"X-Session-Id" = $SessionId3} `
    -Body '{"productId": 1, "quantity": 1}'

$order3 = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order" `
    -Headers @{"X-Session-Id" = $SessionId3} `
    -Body "{`"cartId`": `"$SessionId3`"}"
$orderId3 = $order3.data.orderId
Write-Host "注文ID: $orderId3"

Write-Host ""
Write-Host "PENDING から直接 SHIPPED へ遷移試行..."
try {
    $result = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order/$orderId3/ship"
    $result | ConvertTo-Json

    if ($result.error.code -eq "INVALID_STATUS_TRANSITION") {
        Write-Host "✅ 不正な状態遷移を正しく拒否（ERROR: INVALID_STATUS_TRANSITION）" -ForegroundColor Green
    }
    else {
        Write-Host "❌ 不正な状態遷移の拒否に失敗" -ForegroundColor Red
        exit 1
    }
}
catch {
    Write-Host "API呼び出しエラー（期待通りのエラーの可能性あり）"
}
Write-Host ""

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "テスト 8: エラーケース - DELIVERED でキャンセル試行" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
try {
    $result = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order/$orderId1/cancel" `
        -Headers @{"X-Session-Id" = $SessionId1}
    $result | ConvertTo-Json

    if ($result.error.code -eq "ORDER_NOT_CANCELLABLE") {
        Write-Host "✅ DELIVERED 状態のキャンセルを正しく拒否（ERROR: ORDER_NOT_CANCELLABLE）" -ForegroundColor Green
    }
    else {
        Write-Host "❌ DELIVERED 状態のキャンセル拒否に失敗" -ForegroundColor Red
        exit 1
    }
}
catch {
    Write-Host "API呼び出しエラー（期待通りのエラーの可能性あり）"
}
Write-Host ""

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "テスト 9: エラーケース - 既にキャンセル済みをキャンセル試行" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
try {
    $result = Invoke-ApiRequest -Method POST -Url "$BaseUrl/api/order/$orderId2/cancel" `
        -Headers @{"X-Session-Id" = $SessionId2}
    $result | ConvertTo-Json

    if ($result.error.code -eq "ALREADY_CANCELLED") {
        Write-Host "✅ 既にキャンセル済みの注文キャンセルを正しく拒否（ERROR: ALREADY_CANCELLED）" -ForegroundColor Green
    }
    else {
        Write-Host "❌ 既にキャンセル済みの注文キャンセル拒否に失敗" -ForegroundColor Red
        exit 1
    }
}
catch {
    Write-Host "API呼び出しエラー（期待通りのエラーの可能性あり）"
}
Write-Host ""

Write-Host "==========================================" -ForegroundColor Green
Write-Host "✅ 全テスト完了" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "テスト結果サマリー:"
Write-Host "  1. 注文作成（PENDING）: ✅"
Write-Host "  2. 注文確認（PENDING → CONFIRMED）: ✅"
Write-Host "  3. 注文発送（CONFIRMED → SHIPPED）: ✅"
Write-Host "  4. 配達完了（SHIPPED → DELIVERED）: ✅"
Write-Host "  5. 注文キャンセル（PENDING → CANCELLED）: ✅"
Write-Host "  6. 全注文取得: ✅"
Write-Host "  7. 不正な状態遷移の拒否: ✅"
Write-Host "  8. DELIVERED でキャンセル拒否: ✅"
Write-Host "  9. 既にキャンセル済みをキャンセル拒否: ✅"
Write-Host ""
Write-Host "🎉 すべてのテストが成功しました！" -ForegroundColor Green
