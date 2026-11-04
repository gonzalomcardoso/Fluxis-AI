package com.fluxisai.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.fluxisai.PostgreSqlIntegrationTestSupport;
import com.fluxisai.common.web.PageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@AutoConfigureTestRestTemplate
class JobOpportunityApiIT extends PostgreSqlIntegrationTestSupport {

    private static final ParameterizedTypeReference<PageResponse<JobOpportunityResponse>> PAGE_TYPE =
            new ParameterizedTypeReference<>() {};

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JobOpportunityRepository jobOpportunityRepository;

    @AfterEach
    void cleanOpportunities() {
        jobOpportunityRepository.deleteAll();
    }

    @Test
    void postPersistsOpportunityAndGetReturnsItFromPostgres() {
        CreateJobOpportunityRequest request = new CreateJobOpportunityRequest(
                "Example Company",
                "Platform Engineer",
                "https://jobs.example.test/platform-e2e",
                "Synthetic",
                null,
                "Synthetic description",
                "Synthetic requirements",
                List.of("Java", "PostgreSQL"),
                JobStatus.INTERESTING);

        ResponseEntity<JobOpportunityResponse> created = restTemplate.postForEntity(
                "/api/job-opportunities", request, JobOpportunityResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        assertThat(created.getBody()).isNotNull();
        String id = created.getBody().id().toString();

        ResponseEntity<JobOpportunityResponse> fetched = restTemplate.getForEntity(
                "/api/job-opportunities/" + id, JobOpportunityResponse.class);
        ResponseEntity<PageResponse<JobOpportunityResponse>> listed = restTemplate.exchange(
                "/api/job-opportunities?page=0&size=10",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                PAGE_TYPE);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody()).isNotNull();
        assertThat(fetched.getBody().companyName()).isEqualTo("Example Company");
        assertThat(fetched.getBody().technologies()).containsExactly("Java", "PostgreSQL");
        assertThat(listed.getBody()).isNotNull();
        assertThat(listed.getBody().content()).extracting(JobOpportunityResponse::id)
                .contains(created.getBody().id());
    }
}
