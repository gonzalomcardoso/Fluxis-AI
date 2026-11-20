package com.fluxisai.proposal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import com.fluxisai.PostgreSqlIntegrationTestSupport;
import com.fluxisai.auth.FluxisUserDetails;
import com.fluxisai.catalog.ProfessionalService;
import com.fluxisai.catalog.ProfessionalServiceRepository;
import com.fluxisai.catalog.ProfessionalServiceStatus;
import com.fluxisai.catalog.PricingModel;
import com.fluxisai.client.Client;
import com.fluxisai.client.ClientRepository;
import com.fluxisai.client.ClientStatus;
import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ProposalApiIT extends PostgreSqlIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private ClientRepository clients;

    @Autowired
    private ProfessionalServiceRepository services;

    @Autowired
    private ProposalRepository proposals;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private Client client;
    private ProfessionalService service;

    @BeforeEach
    void setUp() {
        user = users.saveAndFlush(new User(
                "proposal-it-" + UUID.randomUUID() + "@fluxis.local",
                "Synthetic Proposal Test User",
                passwordEncoder.encode("synthetic-proposal-password")));
        client = clients.saveAndFlush(new Client(
                user.getId(), null, null, "Synthetic Client", ClientStatus.POTENCIAL, null, null));
        service = services.saveAndFlush(new ProfessionalService(
                user.getId(),
                "Synthetic USD Service",
                null,
                null,
                PricingModel.HOURLY,
                new BigDecimal("100.0000"),
                "USD",
                true,
                ProfessionalServiceStatus.ACTIVE,
                null));
    }

    @AfterEach
    void cleanDatabase() {
        proposals.findAllByUserId(user.getId(), Pageable.unpaged()).forEach(proposals::delete);
        clients.findAllByUserId(user.getId(), Pageable.unpaged()).forEach(clients::delete);
        services.findAllByUserId(user.getId(), Pageable.unpaged()).forEach(services::delete);
        users.delete(user);
    }

    @Test
    void rejectsProposalWhenServiceCurrencyDoesNotMatchProposalCurrency() throws Exception {
        mockMvc.perform(post("/api/proposals")
                        .with(user(FluxisUserDetails.from(user)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "Currency mismatch",
                                  "currency": "EUR",
                                  "items": [{
                                    "serviceId": "%s",
                                    "description": "Synthetic item",
                                    "quantity": 1,
                                    "unitPrice": 100.00
                                  }]
                                }
                                """.formatted(client.getId(), service.getId())))
                .andExpect(status().isBadRequest());

        assertThat(proposals.findAllByUserId(user.getId(), Pageable.unpaged())).isEmpty();
    }

    @Test
    void createsProposalWhenCurrencyMatchesAndPersistsRoundedTotal() throws Exception {
        mockMvc.perform(post("/api/proposals")
                        .with(user(FluxisUserDetails.from(user)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "%s",
                                  "title": "Currency match",
                                  "currency": "USD",
                                  "items": [{
                                    "serviceId": "%s",
                                    "description": "Synthetic item",
                                    "quantity": 2.5,
                                    "unitPrice": 100.00
                                  }]
                                }
                                """.formatted(client.getId(), service.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.total").isNumber());

        var saved = proposals.findAllByUserId(user.getId(), Pageable.unpaged()).getContent().getFirst();
        assertThat(saved.getTotal()).isEqualByComparingTo("250.00");
        assertThat(saved.getTotal().scale()).isEqualTo(2);
    }
}