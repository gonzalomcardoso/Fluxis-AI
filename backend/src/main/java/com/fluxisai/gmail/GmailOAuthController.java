package com.fluxisai.gmail;

import java.net.URI;

import com.fluxisai.common.error.ConflictException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/gmail/oauth")
public class GmailOAuthController {

    private final GmailOAuthService oauthService;
    private final String frontendBaseUrl;

    public GmailOAuthController(
            GmailOAuthService oauthService,
            @Value("${APP_FRONTEND_URL:http://localhost:5173}") String frontendBaseUrl) {
        this.oauthService = oauthService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @GetMapping("/start")
    @Operation(summary = "Start the Gmail OAuth flow")
    @SecurityRequirement(name = "sessionCookie")
    public ResponseEntity<Void> start() {
        URI authorizationUri = oauthService.startAuthorization();
        return ResponseEntity.status(HttpStatus.FOUND).location(authorizationUri).build();
    }

    @GetMapping("/callback")
    @Operation(summary = "Complete the Gmail OAuth flow")
    @SecurityRequirement(name = "sessionCookie")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        if (error != null || isBlank(code) || isBlank(state)) {
            return redirect("error");
        }

        try {
            oauthService.completeAuthorization(code, state);
            return redirect("connected");
        } catch (ConflictException exception) {
            return redirect("conflict");
        } catch (RuntimeException exception) {
            return redirect("error");
        }
    }

    private ResponseEntity<Void> redirect(String result) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(frontendRedirect(result))
                .build();
    }

    private URI frontendRedirect(String result) {
        URI base = URI.create(frontendBaseUrl);
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getHost() == null || base.getHost().isBlank()) {
            throw new IllegalStateException("Frontend URL is invalid");
        }

        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .scheme(base.getScheme())
                .host(base.getHost());
        if (base.getPort() >= 0) {
            builder.port(base.getPort());
        }
        return builder.path("/dashboard")
                .queryParam("gmail", result)
                .build()
                .toUri();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}