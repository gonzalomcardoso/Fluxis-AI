package com.fluxisai.gmail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GmailConnectionController.class)
@AutoConfigureMockMvc
@WithMockUser
class GmailConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConnectedAccountService connectedAccounts;

    @MockitoBean
    private GmailConnectionService connectionService;

    @Test
    void statusReturnsPublicConnectionMetadataWithoutTokens() throws Exception {
        when(connectedAccounts.currentUserConnection()).thenReturn(Optional.of(response("person@example.test")));

        mockMvc.perform(get("/api/gmail/connection/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountEmail").value("person@example.test"))
                .andExpect(jsonPath("$.providerAccountId").value("google-sub-123"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.accessTokenEncrypted").doesNotExist())
                .andExpect(jsonPath("$.refreshTokenEncrypted").doesNotExist());
    }

    @Test
    void statusReturnsNoContentWhenNoAccountIsConnected() throws Exception {
        when(connectedAccounts.currentUserConnection()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/gmail/connection/status"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void statusIsScopedToTheAuthenticatedUser() throws Exception {
        when(connectedAccounts.currentUserConnection()).thenAnswer(invocation -> {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return "alice".equals(username)
                    ? Optional.of(response("alice@example.test"))
                    : Optional.empty();
        });

        mockMvc.perform(get("/api/gmail/connection/status").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountEmail").value("alice@example.test"));
        mockMvc.perform(get("/api/gmail/connection/status").with(user("bob")))
                .andExpect(status().isNoContent());
    }

    @Test
    void disconnectRequiresCsrfAndDelegatesWhenPresent() throws Exception {
        mockMvc.perform(delete("/api/gmail/connection").with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(connectionService).disconnectCurrentUser();
    }

    @Test
    void disconnectWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(delete("/api/gmail/connection"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(connectionService);
    }

    private ConnectedAccountResponse response(String email) {
        return new ConnectedAccountResponse(
                UUID.randomUUID(),
                ConnectedAccountProvider.GOOGLE,
                "google-sub-123",
                email,
                List.of(GmailOAuthScopes.GMAIL_METADATA),
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1),
                ConnectedAccountStatus.CONNECTED,
                OffsetDateTime.now(ZoneOffset.UTC));
    }
}