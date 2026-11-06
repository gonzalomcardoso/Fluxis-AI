package com.fluxisai.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fluxisai.PostgreSqlIntegrationTestSupport;
import com.fluxisai.auth.FluxisUserDetails;
import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class JobOpportunityApiIT extends PostgreSqlIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobOpportunityRepository jobOpportunityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void createUser() {
        user = userRepository.saveAndFlush(new User(
                "api-it-" + UUID.randomUUID() + "@fluxis.local",
                "Synthetic API Test User",
                passwordEncoder.encode("synthetic-api-test-password")));
    }

    @AfterEach
    void cleanDatabase() {
        jobOpportunityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void postPersistsOpportunityAndGetReturnsItFromPostgres() throws Exception {
        FluxisUserDetails principal = FluxisUserDetails.from(user);
        MvcResult created = mockMvc.perform(post("/api/job-opportunities")
                        .with(user(principal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "Example Company",
                                  "title": "Platform Engineer",
                                  "url": "https://jobs.example.test/platform-e2e",
                                  "source": "Synthetic",
                                  "description": "Synthetic description",
                                  "requirements": "Synthetic requirements",
                                  "technologies": ["Java", "PostgreSQL"],
                                  "status": "INTERESTING"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.companyName").value("Example Company"))
                .andReturn();

        String location = created.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        String id = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/job-opportunities/" + id).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Example Company"))
                .andExpect(jsonPath("$.technologies[0]").value("Java"))
                .andExpect(jsonPath("$.technologies[1]").value("PostgreSQL"));

        mockMvc.perform(get("/api/job-opportunities?page=0&size=10").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.totalElements").value(1));

        assertThat(jobOpportunityRepository.findAllByUserId(
                user.getId(), org.springframework.data.domain.Pageable.unpaged()))
                .hasSize(1);
    }
}
