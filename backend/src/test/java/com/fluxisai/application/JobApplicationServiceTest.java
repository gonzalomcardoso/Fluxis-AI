package com.fluxisai.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import com.fluxisai.common.error.ConflictException;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.job.JobOpportunity;
import com.fluxisai.job.JobOpportunityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OPPORTUNITY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    @Mock JobApplicationRepository applications; @Mock JobOpportunityRepository opportunities; @Mock CurrentUser currentUser; @InjectMocks JobApplicationService service;

    @Test void createsApplicationForCurrentUsersOpportunity() {
        when(currentUser.id()).thenReturn(USER_ID);
        JobOpportunity opportunity = new JobOpportunity(USER_ID, "Acme", "Backend", null, null, null, null, null, null, null);
        when(opportunities.findByIdAndUserId(OPPORTUNITY_ID, USER_ID)).thenReturn(Optional.of(opportunity));
        when(applications.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var response = service.create(new CreateJobApplicationRequest(OPPORTUNITY_ID, null, null, "  notes  "));
        assertEquals("Acme", response.companyName()); assertEquals(ApplicationStatus.PENDING, response.status());
        verify(applications).save(any(JobApplication.class));
    }

    @Test void rejectsDuplicateApplication() {
        when(currentUser.id()).thenReturn(USER_ID);
        JobOpportunity opportunity = new JobOpportunity(USER_ID, "Acme", "Backend", null, null, null, null, null, null, null);
        when(opportunities.findByIdAndUserId(OPPORTUNITY_ID, USER_ID)).thenReturn(Optional.of(opportunity));
        when(applications.existsByUserIdAndJobOpportunityId(USER_ID, OPPORTUNITY_ID)).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(new CreateJobApplicationRequest(OPPORTUNITY_ID, null, null, null)));
        verify(applications, never()).save(any());
    }
}
