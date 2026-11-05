package com.fluxisai.common.security;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import com.fluxisai.job.JobOpportunity;
import com.fluxisai.job.JobOpportunityRepository;
import com.fluxisai.job.JobStatus;
import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@Profile("test-data")
public class ManualTestDataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManualTestDataSeeder.class);
    private static final int OPPORTUNITY_COUNT = 120;
    private static final String TEST_EMAIL = "integration@fluxis.local";
    private static final List<String> TITLES = List.of(
            "Java Developer",
            "Software Engineer",
            "Frontend Developer",
            "Data Engineer",
            "QA Specialist",
            "DevOps Engineer",
            "UX Designer",
            "Systems Analyst");
    private static final List<String> SOURCES = List.of(
            "Synthetic Job Board A",
            "Synthetic Job Board B",
            "Synthetic Job Board C");

    private final UserRepository userRepository;
    private final JobOpportunityRepository jobOpportunityRepository;

    public ManualTestDataSeeder(
            UserRepository userRepository,
            JobOpportunityRepository jobOpportunityRepository) {
        this.userRepository = userRepository;
        this.jobOpportunityRepository = jobOpportunityRepository;
    }

    @Override
    public void run(String... args) {
        User user = userRepository.findByEmailIgnoreCase(TEST_EMAIL)
                .orElseGet(() -> userRepository.save(new User(TEST_EMAIL, "Synthetic Integration User")));
        List<JobOpportunity> missingOpportunities = new ArrayList<>();
        JobStatus[] statuses = JobStatus.values();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        for (int index = 0; index < OPPORTUNITY_COUNT; index++) {
            String url = "https://jobs.example.test/manual-test/" + index;
            if (!jobOpportunityRepository.existsByUserIdAndUrl(user.getId(), url)) {
                missingOpportunities.add(new JobOpportunity(
                        user.getId(),
                        "Sample Company %02d".formatted(index % 12 + 1),
                        TITLES.get(index % TITLES.size()),
                        url,
                        SOURCES.get(index % SOURCES.size()),
                        today.minusDays(index % 90),
                        "Synthetic description for testing lists, filters, and pagination.",
                        "Fictional requirements: relevant experience and knowledge of the field.",
                        technologiesFor(index),
                        statuses[index % statuses.length]));
            }
        }

        jobOpportunityRepository.saveAll(missingOpportunities);
        LOGGER.info("Added {} synthetic manual-test opportunities", missingOpportunities.size());
    }

    private List<String> technologiesFor(int index) {
        return switch (index % 4) {
            case 0 -> List.of("Java", "Spring Boot", "PostgreSQL");
            case 1 -> List.of("TypeScript", "React", "Vite");
            case 2 -> List.of("Python", "SQL", "Data Engineering");
            default -> List.of("Docker", "CI/CD", "Cloud");
        };
    }
}
