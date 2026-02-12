/**
 * エラーメッセージマッピング
 * バックエンドのエラーコードをユーザーフレンドリーなメッセージに変換する
 */

interface ErrorMessageConfig {
  message: string
  action?: string
}

const ERROR_MESSAGES: Record<string, ErrorMessageConfig> = {
  // ネットワーク・システムエラー
  NETWORK_ERROR: {
    message: 'ネットワークエラーが発生しました',
    action: '接続を確認してもう一度お試しください',
  },
  INTERNAL_ERROR: {
    message: '一時的なエラーが発生しました',
    action: 'しばらくしてからもう一度お試しください',
  },
  INVALID_REQUEST: {
    message: '無効なリクエストです',
    action: '入力内容を確認してください',
  },

  // カート関連
  CART_NOT_FOUND: {
    message: 'カート情報が見つかりませんでした',
    action: 'ページを更新してください',
  },
  CART_EMPTY: {
    message: 'カートに商品がありません',
    action: '商品を追加してください',
  },
  RESERVATION_NOT_FOUND: {
    message: 'カート情報が見つかりませんでした',
    action: 'ページを更新してください',
  },
  NO_RESERVATIONS: {
    message: 'カート情報が見つかりませんでした',
    action: 'ページを更新してください',
  },

  // 商品関連
  ITEM_NOT_FOUND: {
    message: '商品が見つかりませんでした',
  },
  ITEM_NOT_AVAILABLE: {
    message: 'この商品は現在購入できません',
    action: 'カートから削除してください',
  },

  // 在庫関連
  OUT_OF_STOCK: {
    message: '在庫が不足している商品があります',
    action: '数量を調整してください',
  },
  INSUFFICIENT_STOCK: {
    message: '在庫が不足しています',
    action: '数量を減らしてお試しください',
  },
  INVALID_QUANTITY: {
    message: '数量が無効です',
    action: '1〜9の範囲で指定してください',
  },

  // 注文関連
  ORDER_NOT_FOUND: {
    message: '注文が見つかりませんでした',
  },
  ORDER_NOT_CANCELLABLE: {
    message: 'この注文はキャンセルできません',
  },
  ALREADY_CANCELLED: {
    message: 'この注文は既にキャンセルされています',
  },
  INVALID_STATUS_TRANSITION: {
    message: '操作を実行できませんでした',
  },
}

/**
 * エラーコードからユーザーフレンドリーなメッセージを取得
 * @param errorCode - バックエンドから返されたエラーコード
 * @returns ユーザー向けエラーメッセージ
 */
export function getUserFriendlyMessage(errorCode: string): string {
  const config = ERROR_MESSAGES[errorCode]

  if (!config) {
    return '操作に失敗しました。もう一度お試しください。'
  }

  // アクションガイダンスがある場合は結合
  if (config.action) {
    return `${config.message}。${config.action}。`
  }

  return config.message
}

/**
 * エラーコードとアクションを個別に取得
 * UI側でメッセージとアクションを別々に表示したい場合に使用
 * @param errorCode - バックエンドから返されたエラーコード
 * @returns メッセージとアクションを含むオブジェクト
 */
export function getErrorMessageWithAction(errorCode: string): {
  message: string
  action?: string
} {
  const config = ERROR_MESSAGES[errorCode]

  if (!config) {
    return {
      message: '操作に失敗しました',
      action: 'もう一度お試しください',
    }
  }

  return {
    message: config.message,
    action: config.action,
  }
}
