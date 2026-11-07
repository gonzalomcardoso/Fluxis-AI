package com.fluxisai.job;

import java.util.UUID;

import com.fluxisai.common.error.ConflictException;
import com.fluxisai.common.error.ResourceNotFoundException;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.common.web.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobOpportunityService {

    private final JobOpportunityRepository jobOpportunityRepository;
    private final CurrentUser currentUser;

    public JobOpportunityService(JobOpportunityRepository jobOpportunityRepository, CurrentUser currentUser) {
        this.jobOpportunityRepository = jobOpportunityRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public JobOpportunityResponse create(CreateJobOpportunityRequest request) {
        UUID userId = currentUser.id();
        if (request.url() != null && jobOpportunityRepository.existsByUserIdAndUrl(userId, request.url())) {
            throw new ConflictException("Ya tienes una oportunidad guardada con esa URL");
        }

        JobOpportunity opportunity = new JobOpportunity(
                userId,
                request.companyName().trim(),
                request.title().trim(),
                request.url(),
                request.source(),
                request.postedOn(),
                request.description(),
                request.requirements(),
                request.technologies(),
                request.status());
        return JobOpportunityResponse.from(jobOpportunityRepository.save(opportunity));
    }

    @Transactional(readOnly = true)
    public JobOpportunityResponse get(UUID id) {
        return jobOpportunityRepository.findByIdAndUserId(id, currentUser.id())
                .map(JobOpportunityResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la oportunidad laboral"));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobOpportunityResponse> list(Pageable pageable) {
        Page<JobOpportunityResponse> page = jobOpportunityRepository
                .findAllByUserId(currentUser.id(), pageable)
                .map(JobOpportunityResponse::from);
        return PageResponse.from(page);
    }
}
