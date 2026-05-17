package com.example.club.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 配置类
 * 提供API文档自动生成和访问功能
 */
@Configuration
public class OpenApiConfig {

	/**
	 * 配置OpenAPI信息
	 */
	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("校园社团管理系统API文档")
				.version("1.0.0")
				.description("校园社团管理系统的RESTful API接口文档，提供用户管理、社团管理、成员管理、活动管理等功能。")
				.contact(new Contact()
					.name("开发团队")
					.email("dev@example.com"))
				.license(new License()
					.name("Apache 2.0")
					.url("https://www.apache.org/licenses/LICENSE-2.0.html")))
			.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
			.components(new io.swagger.v3.oas.models.Components()
				.addSecuritySchemes("Bearer Authentication",
					new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")
						.description("请输入JWT Token，格式：Bearer {token}")));
	}
}

