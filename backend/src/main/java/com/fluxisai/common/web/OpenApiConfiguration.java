package com.fluxisai.common.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenApiCustomizer logoutOperation() {
        return openApi -> openApi.path("/api/auth/logout", new PathItem().post(new Operation()
                .operationId("logout")
                .summary("End the current session")
                .description("Invalidates the server-side session and clears the session cookie.")
                .addParametersItem(new Parameter()
                        .name("X-CSRF-TOKEN")
                        .in("header")
                        .required(true)
                        .description("Token returned by GET /api/auth/csrf."))
                .addSecurityItem(new SecurityRequirement().addList("sessionCookie").addList("csrfToken"))
                .responses(new ApiResponses()
                        .addApiResponse("204", new ApiResponse().description("Session ended"))
                        .addApiResponse("403", new ApiResponse().description("Missing or invalid CSRF token")))));
    }

    @Bean
    OpenAPI fluxisOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fluxis AI API")
                        .version("v1")
                        .description("Authenticated endpoints use an HTTP session cookie. State-changing requests also require the CSRF token returned by GET /api/auth/csrf in the header named by that response."))
                .components(new Components()
                        .addSecuritySchemes("sessionCookie", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("JSESSIONID"))
                        .addSecuritySchemes("csrfToken", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-CSRF-TOKEN")));
    }
}
