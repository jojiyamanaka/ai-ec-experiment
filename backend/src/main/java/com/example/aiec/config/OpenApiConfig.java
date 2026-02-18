package com.example.aiec.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI EC Core API")
                        .version("1.0.0")
                        .description("AI EC Experiment の Core API 仕様"))
                .addSecurityItem(new SecurityRequirement()
                        .addList("SessionId")
                        .addList("BearerAuth")
                        .addList("BoAuth"))
                .schemaRequirement("SessionId", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Session-Id")
                        .description("セッションID（カート・注文操作に必要）"))
                .schemaRequirement("BearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description("顧客認証トークン"))
                .schemaRequirement("BoAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description("管理者認証トークン"));
    }
}
