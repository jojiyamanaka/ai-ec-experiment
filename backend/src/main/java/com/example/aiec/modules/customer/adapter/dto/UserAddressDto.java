package com.example.aiec.modules.customer.adapter.dto;

import com.example.aiec.modules.customer.domain.entity.UserAddress;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "会員住所情報")
public class UserAddressDto {
    @Schema(description = "住所ID", example = "1")
    private Long id;
    @Schema(description = "住所ラベル", example = "自宅")
    private String label;
    @Schema(description = "受取人名", example = "田中 太郎")
    private String recipientName;
    @Schema(description = "受取人電話番号", example = "09012345678")
    private String recipientPhoneNumber;
    @Schema(description = "郵便番号", example = "1000001")
    private String postalCode;
    @Schema(description = "都道府県", example = "東京都")
    private String prefecture;
    @Schema(description = "市区町村", example = "千代田区")
    private String city;
    @Schema(description = "住所1", example = "千代田1-1")
    private String addressLine1;
    @Schema(description = "住所2", example = "皇居前ビル 10F")
    private String addressLine2;
    @Schema(description = "デフォルト住所", example = "true")
    private Boolean isDefault;
    @Schema(description = "表示順", example = "0")
    private Integer addressOrder;

    public static UserAddressDto fromEntity(UserAddress address) {
        UserAddressDto dto = new UserAddressDto();
        dto.setId(address.getId());
        dto.setLabel(address.getLabel());
        dto.setRecipientName(address.getRecipientName());
        dto.setRecipientPhoneNumber(address.getRecipientPhoneNumber());
        dto.setPostalCode(address.getPostalCode());
        dto.setPrefecture(address.getPrefecture());
        dto.setCity(address.getCity());
        dto.setAddressLine1(address.getAddressLine1());
        dto.setAddressLine2(address.getAddressLine2());
        dto.setIsDefault(address.getIsDefault());
        dto.setAddressOrder(address.getAddressOrder());
        return dto;
    }
}
