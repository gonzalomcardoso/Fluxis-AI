package com.fluxisai.application;

import java.util.List;
import java.util.UUID;

import com.fluxisai.common.error.ResourceNotFoundException;
import com.fluxisai.common.security.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationInterviewService {
    private final ApplicationInterviewRepository interviews;
    private final JobApplicationRepository applications;
    private final CurrentUser currentUser;

    public ApplicationInterviewService(ApplicationInterviewRepository interviews, JobApplicationRepository applications, CurrentUser currentUser) { this.interviews = interviews; this.applications = applications; this.currentUser = currentUser; }

    @Transactional(readOnly = true)
    public List<InterviewResponse> list(UUID applicationId) { application(applicationId); return interviews.findAllByJobApplicationIdAndUserId(applicationId, currentUser.id(), Sort.by(Sort.Direction.ASC, "scheduledAt")).stream().map(InterviewResponse::from).toList(); }

    @Transactional
    public InterviewResponse create(UUID applicationId, InterviewRequest request) { application(applicationId); return InterviewResponse.from(interviews.save(new ApplicationInterview(currentUser.id(), applicationId, request.scheduledAt(), request.interviewType().trim(), optional(request.notes())))); }

    @Transactional
    public InterviewResponse update(UUID applicationId, UUID id, InterviewRequest request) { ApplicationInterview interview = find(applicationId, id); interview.update(request.scheduledAt(), request.interviewType().trim(), optional(request.notes())); return InterviewResponse.from(interviews.save(interview)); }

    @Transactional
    public void delete(UUID applicationId, UUID id) { interviews.delete(find(applicationId, id)); }

    private ApplicationInterview find(UUID applicationId, UUID id) { application(applicationId); return interviews.findByIdAndJobApplicationIdAndUserId(id, applicationId, currentUser.id()).orElseThrow(() -> new ResourceNotFoundException("No se encontró la entrevista")); }
    private void application(UUID id) { applications.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new ResourceNotFoundException("No se encontró la candidatura")); }
    private static String optional(String value) { if (value == null) return null; String normalized=value.trim(); return normalized.isEmpty()?null:normalized; }
}
