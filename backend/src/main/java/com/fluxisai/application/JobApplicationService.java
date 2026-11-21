package com.fluxisai.application;

import java.util.UUID;

import com.fluxisai.common.error.ConflictException;
import com.fluxisai.common.error.InvalidRequestException;
import com.fluxisai.common.error.ResourceNotFoundException;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.common.web.PageResponse;
import com.fluxisai.job.JobOpportunity;
import com.fluxisai.job.JobOpportunityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobApplicationService {
    private final JobApplicationRepository applications;
    private final JobOpportunityRepository opportunities;
    private final CurrentUser currentUser;

    public JobApplicationService(JobApplicationRepository applications, JobOpportunityRepository opportunities, CurrentUser currentUser) {
        this.applications = applications;
        this.opportunities = opportunities;
        this.currentUser = currentUser;
    }

    @Transactional
    public JobApplicationResponse create(CreateJobApplicationRequest request) {
        UUID userId = currentUser.id();
        JobOpportunity opportunity = opportunity(request.jobOpportunityId());
        if (applications.existsByUserIdAndJobOpportunityId(userId, request.jobOpportunityId())) {
            throw new ConflictException("Ya existe una candidatura para esta oportunidad");
        }
        return response(applications.save(new JobApplication(userId, request.jobOpportunityId(), request.status(), request.appliedOn(), optional(request.notes()))), opportunity);
    }

    @Transactional(readOnly = true)
    public JobApplicationResponse get(UUID id) {
        JobApplication application = find(id);
        return response(application, opportunity(application.getJobOpportunityId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobApplicationResponse> list(Pageable pageable) {
        Page<JobApplicationResponse> page = applications.findAllByUserId(currentUser.id(), pageable)
                .map(application -> response(application, opportunity(application.getJobOpportunityId())));
        return PageResponse.from(page);
    }

    @Transactional
    public JobApplicationResponse update(UUID id, UpdateJobApplicationRequest request) {
        JobApplication application = find(id);
        validateTransition(application.getStatus(), request.status());
        application.update(request.status(), request.appliedOn(), optional(request.notes()));
        return response(applications.save(application), opportunity(application.getJobOpportunityId()));
    }

    @Transactional
    public void delete(UUID id) {
        applications.delete(find(id));
    }

    private JobApplication find(UUID id) {
        return applications.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la candidatura"));
    }

    private JobOpportunity opportunity(UUID id) {
        return opportunities.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la oportunidad laboral"));
    }

    private static JobApplicationResponse response(JobApplication application, JobOpportunity opportunity) {
        return new JobApplicationResponse(application.getId(), application.getJobOpportunityId(), opportunity.getCompanyName(), opportunity.getTitle(), application.getStatus(), application.getAppliedOn(), application.getNotes(), application.getCreatedAt(), application.getUpdatedAt());
    }

    private static String optional(String value) { if (value == null) return null; String normalized = value.trim(); return normalized.isEmpty() ? null : normalized; }

    private static void validateTransition(ApplicationStatus current, ApplicationStatus next) {
        if (next == null || next == current) return;
        boolean allowed = switch (current) {
            case PENDING -> next != null;
            case INTERESTING -> next != ApplicationStatus.PENDING;
            case PREPARING_APPLICATION -> next == ApplicationStatus.APPLICATION_SENT || next == ApplicationStatus.INTERVIEW || next == ApplicationStatus.OFFER_RECEIVED || next == ApplicationStatus.REJECTED || next == ApplicationStatus.DISCARDED;
            case APPLICATION_SENT -> next == ApplicationStatus.INTERVIEW || next == ApplicationStatus.OFFER_RECEIVED || next == ApplicationStatus.REJECTED || next == ApplicationStatus.DISCARDED;
            case INTERVIEW -> next == ApplicationStatus.OFFER_RECEIVED || next == ApplicationStatus.REJECTED || next == ApplicationStatus.DISCARDED;
            case OFFER_RECEIVED, REJECTED, DISCARDED -> false;
        };
        if (!allowed) throw new InvalidRequestException("El cambio de estado de la candidatura no es válido");
    }
}
