package com.fluxisai.common.security;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fluxisai.job.JobOpportunity;
import com.fluxisai.job.JobOpportunityRepository;
import com.fluxisai.job.JobStatus;
import com.fluxisai.user.User;
import com.fluxisai.user.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements org.springframework.boot.CommandLineRunner {

    private final UserRepository userRepository;
    private final JobOpportunityRepository jobOpportunityRepository;

    public DevDataSeeder(
            UserRepository userRepository,
            JobOpportunityRepository jobOpportunityRepository) {
        this.userRepository = userRepository;
        this.jobOpportunityRepository = jobOpportunityRepository;
    }

    @Override
    public void run(String... args) {
        seedUser(
                "dev@fluxis.local",
                "Develop user",
                List.of(
                        new SeedOpportunity("Example Labs", "Java Developer", "java-dev", JobStatus.INTERESTING),
                        new SeedOpportunity("Sample Systems", "Platform Engineer", "platform-dev", JobStatus.PENDING)));
        seedUser(
                "demo@fluxis.local",
                "Demo user",
                List.of(
                        new SeedOpportunity("Demo Robotics", "API Developer", "api-demo", JobStatus.PENDING),
                        new SeedOpportunity("Fictional Cloud", "Backend Engineer", "backend-demo", JobStatus.INTERESTING)));
    }

    private void seedUser(String email, String displayName, List<SeedOpportunity> opportunities) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepository.save(new User(email, displayName)));
        if (jobOpportunityRepository.findAllByUserId(user.getId(), Pageable.unpaged()).isEmpty()) {
            opportunities.stream()
                    .map(opportunity -> opportunity.toEntity(user.getId()))
                    .forEach(jobOpportunityRepository::save);
        }
    }

    private record SeedOpportunity(String company, String title, String slug, JobStatus status) {

        private JobOpportunity toEntity(UUID userId) {
            return new JobOpportunity(
                    userId,
                    company,
                    title,
                    "https://jobs.example.test/" + slug,
                    "Synthetic data",
                    LocalDate.of(2026, 11, 1),
                    "Fictional job opportunity for local development.",
                    "Synthetic sample data; this does not represent a real company.",
                    List.of("Java", "PostgreSQL"),
                    status);
        }
    }
}
