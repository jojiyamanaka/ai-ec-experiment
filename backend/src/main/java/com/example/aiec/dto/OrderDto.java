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
        List<OrderItemDto> items = order.getItems().stream()
                .map(OrderItemDto::fromEntity)
                .collect(Collectors.toList());

        return new OrderDto(
                order.getId(),
                order.getOrderNumber(),
                items,
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getCreatedAt().atZone(ZoneId.of("Asia/Tokyo")).format(ISO_FORMATTER),
                order.getUpdatedAt().atZone(ZoneId.of("Asia/Tokyo")).format(ISO_FORMATTER)
        );
    }

}
