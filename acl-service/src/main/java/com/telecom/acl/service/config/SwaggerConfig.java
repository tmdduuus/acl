package com.telecom.acl.service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("통신사 레거시 시스템 연동 API")
                        .description("Anti-Corruption Layer를 통한 레거시 시스템 연동 API입니다.")
                        .version("1.0.0"));
    }
}