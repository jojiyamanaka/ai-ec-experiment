package com.example.aiec.dto;

import com.example.aiec.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class OrderDto {

    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private String userDisplayName;
    private List<OrderItemDto> items;
    private Integer totalPrice;
    private String status;
    private String createdAt;
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
