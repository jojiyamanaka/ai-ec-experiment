package com.example.aiec.modules.purchase.application.port;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnavailableProductDetail {

    private Long productId;

    private String productName;

}
