export interface Product {
  id: number
  name: string
  price: number
  image: string
  description: string
}

export const mockProducts: Product[] = [
  {
    id: 1,
    name: 'ワイヤレスイヤホン',
    price: 8980,
    image: 'https://placehold.co/400x300/3b82f6/ffffff?text=Product+1',
    description: '高音質で長時間バッテリー対応のワイヤレスイヤホン',
  },
  {
    id: 2,
    name: 'スマートウォッチ',
    price: 15800,
    image: 'https://placehold.co/400x300/8b5cf6/ffffff?text=Product+2',
    description: '健康管理機能が充実したスマートウォッチ',
  },
  {
    id: 3,
    name: 'ポータブル充電器',
    price: 3980,
    image: 'https://placehold.co/400x300/ec4899/ffffff?text=Product+3',
    description: '大容量20000mAhのモバイルバッテリー',
  },
  {
    id: 4,
    name: 'Bluetooth スピーカー',
    price: 6980,
    image: 'https://placehold.co/400x300/10b981/ffffff?text=Product+4',
    description: '防水機能付きポータブルスピーカー',
  },
  {
    id: 5,
    name: 'ワイヤレスマウス',
    price: 2980,
    image: 'https://placehold.co/400x300/f59e0b/ffffff?text=Product+5',
    description: '静音設計の高精度ワイヤレスマウス',
  },
  {
    id: 6,
    name: 'USB-C ハブ',
    price: 4580,
    image: 'https://placehold.co/400x300/ef4444/ffffff?text=Product+6',
    description: '多機能7-in-1 USB-Cハブ',
  },
]
