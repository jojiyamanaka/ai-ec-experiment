package com.example.aiec.modules.customer.adapter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会員登録リクエスト
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "メールアドレスの形式が正しくありません")
    private String email;

    @NotBlank(message = "表示名は必須です")
    @Size(min = 1, max = 100, message = "表示名は1〜100文字である必要があります")
    private String displayName;

    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, max = 100, message = "パスワードは8〜100文字である必要があります")
    private String password;

}
