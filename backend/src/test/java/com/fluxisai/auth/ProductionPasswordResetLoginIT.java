package com.fluxisai.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fluxisai.PostgreSqlIntegrationTestSupport;
import com.fluxisai.common.security.ProductionAdminPasswordService;
import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.cors.allowed-origins=http://localhost:5175,http://127.0.0.1:5175")
@AutoConfigureMockMvc
class ProductionPasswordResetLoginIT extends PostgreSqlIntegrationTestSupport {

    private final String email = "password-reset-" + UUID.randomUUID() + "@fluxis.local";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ProductionAdminPasswordService passwordService;

    @AfterEach
    void removeTestUser() {
        userRepository.findByEmailIgnoreCase(email).ifPresent(userRepository::delete);
    }

    @Test
    void passwordSetByProductionResetServiceCanAuthenticateThroughLoginEndpoint() throws Exception {
        String initialPassword = "initial-password-123";
        String resetPassword = "abcdefghijkl";
        User user = userRepository.saveAndFlush(new User(
                email,
                "Synthetic Reset Test User",
                passwordEncoder.encode(initialPassword)));

        passwordService.updatePassword(user, resetPassword);

        User updated = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(passwordEncoder.matches(resetPassword, updated.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(initialPassword, updated.getPasswordHash())).isFalse();

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, resetPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void corsAllowsBothLocalhostAndLoopbackFrontendOrigins() throws Exception {
        for (String origin : new String[] {"http://localhost:5175", "http://127.0.0.1:5175"}) {
            mockMvc.perform(options("/api/auth/login")
                            .header("Origin", origin)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "content-type,x-csrf-token"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origin));
        }
    }
}
