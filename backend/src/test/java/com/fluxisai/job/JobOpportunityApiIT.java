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
import java.util.ArrayList;
import java.util.List;

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

    private final List<User> testUsers = new ArrayList<>();
    private User user;

    @BeforeEach
    void createUser() {
        user = createTestUser();
    }

    private User createTestUser() {
        User testUser = userRepository.saveAndFlush(new User(
                "api-it-" + UUID.randomUUID() + "@fluxis.local",
                "Synthetic API Test User",
                passwordEncoder.encode("synthetic-api-test-password")));
        testUsers.add(testUser);
        return testUser;
    }

    @AfterEach
    void cleanDatabase() {
        testUsers.forEach(testUser -> {
            jobOpportunityRepository.deleteByUserId(testUser.getId());
            userRepository.delete(testUser);
        });
        testUsers.clear();
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
        assertThat(jobOpportunityRepository.findByIdAndUserId(UUID.fromString(id), user.getId()))
                .isPresent();

        mockMvc.perform(get("/api/job-opportunities/" + id).with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Example Company"))
                .andExpect(jsonPath("$.technologies[0]").value("Java"))
                .andExpect(jsonPath("$.technologies[1]").value("PostgreSQL"));

        mockMvc.perform(get("/api/job-opportunities?page=0&size=10").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void usersCannotReadAnotherUsersOpportunities() throws Exception {
        User otherUser = createTestUser();
        MvcResult created = mockMvc.perform(post("/api/job-opportunities")
                        .with(user(FluxisUserDetails.from(user)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "companyName": "Private Company",
                                  "title": "Private Opportunity",
                                  "url": "https://jobs.example.test/private",
                                  "status": "PENDING"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String location = created.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        String id = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/job-opportunities/" + id)
                        .with(user(FluxisUserDetails.from(otherUser))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/job-opportunities")
                        .with(user(FluxisUserDetails.from(otherUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void openApiSpecificationIsPublicAndDescribesJobOpportunityEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Fluxis AI API"))
                .andExpect(jsonPath("$.paths['/api/job-opportunities']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/logout'].post").exists())
                .andExpect(jsonPath("$.components.securitySchemes.sessionCookie.name").value("JSESSIONID"));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
