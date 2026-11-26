package com.fluxisai.gmail;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;

import com.fluxisai.common.error.GlobalExceptionHandler;
import com.fluxisai.common.error.InvalidRequestException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GmailOAuthController.class)
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
@WithMockUser
@TestPropertySource(properties = "APP_FRONTEND_URL=http://frontend.example.test:5173")
class GmailOAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GmailOAuthService oauthService;

    @Test
    void startRedirectsToTheGoogleAuthorizationUri() throws Exception {
        URI authorizationUri = URI.create("https://accounts.google.com/o/oauth2/v2/auth?state=test");
        when(oauthService.startAuthorization()).thenReturn(authorizationUri);

        mockMvc.perform(get("/api/gmail/oauth/start"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", authorizationUri.toString()))
                .andExpect(content().string(""));

        verify(oauthService).startAuthorization();
    }

    @Test
    void startConfigurationErrorReturnsSafeInternalError() throws Exception {
        when(oauthService.startAuthorization())
                .thenThrow(new IllegalStateException("SENTINEL-CONFIG"));

        mockMvc.perform(get("/api/gmail/oauth/start"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("SENTINEL-CONFIG"))));
    }

    @Test
    void callbackSuccessRedirectsToConfiguredFrontendRoute() throws Exception {
        when(oauthService.completeAuthorization("auth-code", "state-value"))
                .thenReturn(response());

        mockMvc.perform(get("/api/gmail/oauth/callback")
                        .param("code", "auth-code")
                        .param("state", "state-value"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "http://frontend.example.test:5173/dashboard?gmail=connected"));

        verify(oauthService).completeAuthorization("auth-code", "state-value");
    }

    @Test
    void callbackWithGoogleErrorRedirectsWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/gmail/oauth/callback")
                        .param("error", "access_denied"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "http://frontend.example.test:5173/dashboard?gmail=error"));

        verifyNoInteractions(oauthService);
    }

    @Test
    void callbackWithMissingParametersRedirectsWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/gmail/oauth/callback"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "http://frontend.example.test:5173/dashboard?gmail=error"));

        verifyNoInteractions(oauthService);
    }

    @Test
    void callbackInvalidStateRedirectsToError() throws Exception {
        when(oauthService.completeAuthorization("auth-code", "bad-state"))
                .thenThrow(new InvalidRequestException("OAuth authorization request is invalid or expired"));

        mockMvc.perform(get("/api/gmail/oauth/callback")
                        .param("code", "auth-code")
                        .param("state", "bad-state"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "http://frontend.example.test:5173/dashboard?gmail=error"));
    }

    @Test
    void callbackInternalErrorDoesNotExposeSensitiveInformation() throws Exception {
        when(oauthService.completeAuthorization("auth-code", "state-value"))
                .thenThrow(new RuntimeException("SENTINEL-INTERNAL"));

        mockMvc.perform(get("/api/gmail/oauth/callback")
                        .param("code", "auth-code")
                        .param("state", "state-value"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "http://frontend.example.test:5173/dashboard?gmail=error"))
                .andExpect(content().string(""));
    }

    @Test
    void callbackDoesNotAllowArbitraryRedirectDestination() throws Exception {
        when(oauthService.completeAuthorization("auth-code", "state-value"))
                .thenReturn(response());

        mockMvc.perform(get("/api/gmail/oauth/callback")
                        .param("code", "auth-code")
                        .param("state", "state-value")
                        .param("redirect", "https://evil.example.test"))
                .andExpect(status().isFound())
                .andExpect(header().string(
                        "Location",
                        "http://frontend.example.test:5173/dashboard?gmail=connected"));
    }

    private ConnectedAccountResponse response() {
        return new ConnectedAccountResponse(
                java.util.UUID.randomUUID(),
                ConnectedAccountProvider.GOOGLE,
                "google-sub-123",
                "person@example.test",
                GmailOAuthScopes.BASELINE,
                null,
                ConnectedAccountStatus.CONNECTED,
                java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
    }
}