package com.example.aiec.modules.purchase.adapter.dto;

import com.example.aiec.modules.purchase.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 注文DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "注文情報")
public class OrderDto {

    @Schema(description = "注文ID", example = "1")
    private Long orderId;
    @Schema(description = "注文番号", example = "ORD-20260218-001")
    private String orderNumber;
    @Schema(description = "会員ID（ゲスト注文の場合null）", example = "1")
    private Long userId;
    @Schema(description = "会員メールアドレス")
    private String userEmail;
    @Schema(description = "会員表示名")
    private String userDisplayName;
    @Schema(description = "注文商品リスト")
    private List<OrderItemDto> items;
    @Schema(description = "合計金額（円）", example = "11940")
    private BigDecimal totalPrice;
    @Schema(description = "注文ステータス", example = "PENDING")
    private String status;
    @Schema(description = "注文日時（ISO 8601）")
    private String createdAt;
    @Schema(description = "更新日時（ISO 8601）")
    private String updatedAt;

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Asia/Tokyo"));

    /**
     * エンティティから DTO を生成
     */
    public static OrderDto fromEntity(Order order) {
        OrderDto dto = new OrderDto();
        dto.setOrderId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());

        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getId());
            dto.setUserEmail(order.getUser().getEmail());
            dto.setUserDisplayName(order.getUser().getDisplayName());
        }

        dto.setItems(order.getItems().stream()
                .map(OrderItemDto::fromEntity)
                .collect(Collectors.toList()));
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus().name());
        dto.setCreatedAt(order.getCreatedAt().atZone(ZoneId.of("Asia/Tokyo")).format(ISO_FORMATTER));
        dto.setUpdatedAt(order.getUpdatedAt().atZone(ZoneId.of("Asia/Tokyo")).format(ISO_FORMATTER));
        return dto;
    }

}
