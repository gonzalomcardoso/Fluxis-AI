package com.fluxisai.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.fluxisai.PostgreSqlIntegrationTestSupport;
import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JobOpportunityRepositoryIT extends PostgreSqlIntegrationTestSupport {

    @Autowired
    private JobOpportunityRepository jobOpportunityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesVersionOneOnFreshPostgresDatabase() {
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1'", Integer.class);

        assertThat(appliedMigrations).isEqualTo(1);
    }

    @Test
    void savesAndQueriesOpportunityByUser() {
        User user = userRepository.saveAndFlush(new User("test-user@fluxis.local", "Synthetic User"));
        JobOpportunity opportunity = jobOpportunityRepository.saveAndFlush(new JobOpportunity(
                user.getId(),
                "Example Company",
                "Platform Engineer",
                "https://jobs.example.test/platform",
                "Synthetic",
                null,
                "Synthetic description",
                null,
                List.of("Java", "PostgreSQL"),
                JobStatus.PENDING));

        assertThat(jobOpportunityRepository.findByIdAndUserId(opportunity.getId(), user.getId()))
                .get()
                .extracting(JobOpportunity::getCompanyName)
                .isEqualTo("Example Company");
        assertThat(jobOpportunityRepository.findAllByUserId(user.getId(), org.springframework.data.domain.PageRequest.of(0, 10)))
                .hasSize(1);
    }
}
