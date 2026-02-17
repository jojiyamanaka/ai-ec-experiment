export interface InventoryDto {
  id: number;
  productName: string;
  stock: number;
  reservedStock: number;
  availableStock: number;
  lastUpdated: string;
}
