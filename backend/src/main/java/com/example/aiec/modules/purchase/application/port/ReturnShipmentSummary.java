package com.example.aiec.modules.purchase.application.port;

import com.example.aiec.modules.purchase.shipment.entity.Shipment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "返品サマリ")
public class ReturnShipmentSummary {

    @Schema(description = "返品Shipment ID", example = "1")
    private Long shipmentId;

    @Schema(description = "返品ステータス", example = "RETURN_PENDING")
    private String status;

    @Schema(description = "返品ステータス表示名", example = "返品待ち")
    private String statusLabel;

    @Schema(description = "作成日時（ISO 8601）")
    private String createdAt;

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Asia/Tokyo"));

    public static ReturnShipmentSummary fromEntity(Shipment shipment) {
        return new ReturnShipmentSummary(
                shipment.getId(),
                shipment.getStatus().name(),
                ReturnShipmentDto.toStatusLabel(shipment.getStatus()),
                shipment.getCreatedAt().atZone(ZoneId.of("Asia/Tokyo")).format(ISO_FORMATTER)
        );
    }
}
