package com.example.aiec.modules.purchase.application.port;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectReturnRequest {

    @NotBlank(message = "拒否理由は必須です")
    private String reason;
}
