package br.com.fiap.boletimplus.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearer-jwt";

    @Bean
    OpenAPI boletimPlusOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Boletim Plus — API Java")
                        .version("1.0.0")
                        .description("Conta, acompanhamento do aluno e análise do boletim com IA. "
                                + "Entre em POST /auth/login, copie o token e use o botão Authorize."))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
