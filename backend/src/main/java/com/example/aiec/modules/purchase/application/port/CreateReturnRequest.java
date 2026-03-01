package com.example.aiec.modules.purchase.application.port;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnRequest {

    @NotBlank(message = "返品理由は必須です")
    private String reason;

    @Valid
    @NotEmpty(message = "返品明細は必須です")
    private List<Item> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NotNull(message = "注文明細IDは必須です")
        @Min(value = 1, message = "注文明細IDは1以上である必要があります")
        private Long orderItemId;

        @NotNull(message = "返品数量は必須です")
        @Min(value = 1, message = "返品数量は1以上である必要があります")
        private Integer quantity;
    }
}
