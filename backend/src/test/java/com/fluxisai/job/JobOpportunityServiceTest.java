package com.fluxisai.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fluxisai.common.error.ConflictException;
import com.fluxisai.common.error.ResourceNotFoundException;
import com.fluxisai.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class JobOpportunityServiceTest {

    private static final UUID USER_ID = UUID.fromString("1e535170-f3f8-4423-8db6-a707b8b148d0");
    private static final UUID OPPORTUNITY_ID = UUID.fromString("b74a71f5-e7b4-4af3-bdae-2c7af17907c9");

    @Mock
    private JobOpportunityRepository repository;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private JobOpportunityService service;

    @BeforeEach
    void setUp() {
        when(currentUser.id()).thenReturn(USER_ID);
    }

    @Test
    void createTrimsCompanyAndTitleAndScopesToCurrentUser() {
        when(repository.save(any(JobOpportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateJobOpportunityRequest request = new CreateJobOpportunityRequest(
                "  Acme  ",
                "  Backend Developer  ",
                "https://jobs.example.test/acme",
                null,
                null,
                null,
                null,
                List.of("Java"),
                JobStatus.INTERESTING);

        JobOpportunityResponse response = service.create(request);

        ArgumentCaptor<JobOpportunity> captor = ArgumentCaptor.forClass(JobOpportunity.class);
        verify(repository).save(captor.capture());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals("Acme", response.companyName());
        assertEquals("Backend Developer", response.title());
        assertEquals(JobStatus.INTERESTING, response.status());
    }

    @Test
    void listUsesCurrentUserAndMapsThePage() {
        JobOpportunity opportunity = opportunity();
        PageRequest pageable = PageRequest.of(0, 10);
        when(repository.findAllByUserId(USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(opportunity), pageable, 1));

        var response = service.list(pageable);

        assertEquals(1, response.totalElements());
        assertEquals("Acme", response.content().getFirst().companyName());
        verify(repository).findAllByUserId(USER_ID, pageable);
    }

    @Test
    void getThrowsNotFoundWhenOpportunityDoesNotBelongToCurrentUser() {
        when(repository.findByIdAndUserId(OPPORTUNITY_ID, USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(OPPORTUNITY_ID));
        verify(repository).findByIdAndUserId(OPPORTUNITY_ID, USER_ID);
    }

    @Test
    void createRejectsDuplicateUrlForCurrentUser() {
        CreateJobOpportunityRequest request = new CreateJobOpportunityRequest(
                "Acme", "Backend Developer", "https://jobs.example.test/acme",
                null, null, null, null, null, null);
        when(repository.existsByUserIdAndUrl(USER_ID, request.url())).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request));
    }

    private JobOpportunity opportunity() {
        return new JobOpportunity(
                USER_ID, "Acme", "Backend Developer", null, null, null, null, null,
                List.of(), JobStatus.PENDING);
    }
}
