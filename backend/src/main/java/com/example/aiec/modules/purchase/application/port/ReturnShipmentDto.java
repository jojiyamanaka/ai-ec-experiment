package com.example.aiec.modules.purchase.application.port;

import com.example.aiec.modules.purchase.order.entity.Order;
import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import com.example.aiec.modules.purchase.shipment.entity.ShipmentItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "返品情報")
public class ReturnShipmentDto {

    @Schema(description = "返品Shipment ID", example = "1")
    private Long shipmentId;

    @Schema(description = "注文ID", example = "1")
    private Long orderId;

    @Schema(description = "注文番号", example = "ORD-0000000001")
    private String orderNumber;

    @Schema(description = "返品ステータス", example = "RETURN_PENDING")
    private String status;

    @Schema(description = "返品ステータス表示名", example = "返品待ち")
    private String statusLabel;

    @Schema(description = "返品理由", example = "商品に傷があった")
    private String reason;

    @Schema(description = "拒否理由", example = "返品期間対象外のため")
    private String rejectionReason;

    @Schema(description = "返品明細")
    private List<Item> items;

    @Schema(description = "作成日時（ISO 8601）")
    private String createdAt;

    @Schema(description = "更新日時（ISO 8601）")
    private String updatedAt;

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Asia/Tokyo"));

    public static ReturnShipmentDto fromEntity(Shipment shipment, Order order) {
        return new ReturnShipmentDto(
                shipment.getId(),
                order.getId(),
                order.getOrderNumber(),
                shipment.getStatus().name(),
                toStatusLabel(shipment.getStatus()),
                shipment.getReason(),
                shipment.getRejectionReason(),
                shipment.getItems().stream().map(Item::fromEntity).toList(),
                shipment.getCreatedAt().atZone(ZoneId.of("Asia/Tokyo")).format(ISO_FORMATTER),
                shipment.getUpdatedAt().atZone(ZoneId.of("Asia/Tokyo")).format(ISO_FORMATTER)
        );
    }

    public static String toStatusLabel(Shipment.ShipmentStatus status) {
        if (status == Shipment.ShipmentStatus.RETURN_PENDING) {
            return "返品待ち";
        }
        if (status == Shipment.ShipmentStatus.RETURN_APPROVED) {
            return "返品承認済";
        }
        if (status == Shipment.ShipmentStatus.RETURN_CONFIRMED) {
            return "返品確定";
        }
        if (status == Shipment.ShipmentStatus.RETURN_CANCELLED) {
            return "返品キャンセル";
        }
        return status.name();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "返品明細")
    public static class Item {
        @Schema(description = "Shipment明細ID", example = "1")
        private Long shipmentItemId;

        @Schema(description = "注文明細ID", example = "1")
        private Long orderItemId;

        @Schema(description = "商品ID", example = "1")
        private Long productId;

        @Schema(description = "商品名", example = "商品名")
        private String productName;

        @Schema(description = "返品数量", example = "2")
        private Integer quantity;

        @Schema(description = "単価", example = "1000")
        private BigDecimal unitPrice;

        @Schema(description = "小計", example = "2000")
        private BigDecimal subtotal;

        public static Item fromEntity(ShipmentItem shipmentItem) {
            return new Item(
                    shipmentItem.getId(),
                    shipmentItem.getOrderItem().getId(),
                    shipmentItem.getProductId(),
                    shipmentItem.getProductName(),
                    shipmentItem.getQuantity(),
                    shipmentItem.getProductPrice(),
                    shipmentItem.getSubtotal()
            );
        }
    }
}
