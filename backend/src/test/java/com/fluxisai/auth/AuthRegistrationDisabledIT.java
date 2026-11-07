package com.fluxisai.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fluxisai.PostgreSqlIntegrationTestSupport;
import com.fluxisai.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.auth.registration-enabled=false")
@AutoConfigureMockMvc
class AuthRegistrationDisabledIT extends PostgreSqlIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registrationIsRejectedAndCreatesNoAccountWhenDisabled() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "blocked-registration@fluxis.local",
                                  "displayName": "Blocked Registration",
                                  "password": "valid-password-123"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        org.assertj.core.api.Assertions.assertThat(
                userRepository.existsByEmailIgnoreCase("blocked-registration@fluxis.local"))
                .isFalse();
    }
}
