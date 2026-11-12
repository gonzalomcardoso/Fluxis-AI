package com.fluxisai.application;

import java.util.List;
import java.util.UUID;

import com.fluxisai.common.error.ResourceNotFoundException;
import com.fluxisai.common.security.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationFollowUpService {
    private final ApplicationFollowUpRepository followUps;
    private final JobApplicationRepository applications;
    private final CurrentUser currentUser;

    public ApplicationFollowUpService(ApplicationFollowUpRepository followUps, JobApplicationRepository applications, CurrentUser currentUser) { this.followUps = followUps; this.applications = applications; this.currentUser = currentUser; }
    @Transactional(readOnly = true) public List<FollowUpResponse> list(UUID applicationId) { application(applicationId); return followUps.findAllByJobApplicationIdAndUserId(applicationId, currentUser.id(), Sort.by(Sort.Direction.ASC, "dueOn")).stream().map(FollowUpResponse::from).toList(); }
    @Transactional public FollowUpResponse create(UUID applicationId, FollowUpRequest request) { application(applicationId); return FollowUpResponse.from(followUps.save(new ApplicationFollowUp(currentUser.id(), applicationId, request.dueOn(), request.description().trim(), request.completed()))); }
    @Transactional public FollowUpResponse update(UUID applicationId, UUID id, FollowUpRequest request) { ApplicationFollowUp followUp=find(applicationId,id); followUp.update(request.dueOn(),request.description().trim(),request.completed()); return FollowUpResponse.from(followUps.save(followUp)); }
    @Transactional public void delete(UUID applicationId, UUID id) { followUps.delete(find(applicationId,id)); }
    private ApplicationFollowUp find(UUID applicationId,UUID id){application(applicationId);return followUps.findByIdAndJobApplicationIdAndUserId(id,applicationId,currentUser.id()).orElseThrow(() -> new ResourceNotFoundException("No se encontró el seguimiento"));}
    private void application(UUID id){applications.findByIdAndUserId(id,currentUser.id()).orElseThrow(() -> new ResourceNotFoundException("No se encontró la candidatura"));}
}
