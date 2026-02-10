export interface Product {
  id: number
  name: string
  price: number
  image: string
  description: string
  stock: number
}

export const mockProducts: Product[] = [
  {
    id: 1,
    name: 'ワイヤレスイヤホン',
    price: 8980,
    image: 'https://placehold.co/400x300/3b82f6/ffffff?text=Product+1',
    description: '高音質で長時間バッテリー対応のワイヤレスイヤホン',
    stock: 12,
  },
  {
    id: 2,
    name: 'スマートウォッチ',
    price: 15800,
    image: 'https://placehold.co/400x300/8b5cf6/ffffff?text=Product+2',
    description: '健康管理機能が充実したスマートウォッチ',
    stock: 3,
  },
  {
    id: 3,
    name: 'ポータブル充電器',
    price: 3980,
    image: 'https://placehold.co/400x300/ec4899/ffffff?text=Product+3',
    description: '大容量20000mAhのモバイルバッテリー',
    stock: 0,
  },
  {
    id: 4,
    name: 'Bluetooth スピーカー',
    price: 6980,
    image: 'https://placehold.co/400x300/10b981/ffffff?text=Product+4',
    description: '防水機能付きポータブルスピーカー',
    stock: 8,
  },
  {
    id: 5,
    name: 'ワイヤレスマウス',
    price: 2980,
    image: 'https://placehold.co/400x300/f59e0b/ffffff?text=Product+5',
    description: '静音設計の高精度ワイヤレスマウス',
    stock: 5,
  },
  {
    id: 6,
    name: 'USB-C ハブ',
    price: 4580,
    image: 'https://placehold.co/400x300/ef4444/ffffff?text=Product+6',
    description: '多機能7-in-1 USB-Cハブ',
    stock: 15,
  },
  {
    id: 7,
    name: 'ノイズキャンセリングヘッドホン',
    price: 25800,
    image: 'https://placehold.co/400x300/06b6d4/ffffff?text=Product+7',
    description: '最高峰のノイズキャンセリング技術搭載',
    stock: 1,
  },
  {
    id: 8,
    name: 'ワイヤレス充電器',
    price: 3480,
    image: 'https://placehold.co/400x300/84cc16/ffffff?text=Product+8',
    description: '3台同時充電可能なワイヤレス充電パッド',
    stock: 20,
  },
]
