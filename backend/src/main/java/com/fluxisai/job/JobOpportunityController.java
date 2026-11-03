package com.fluxisai.job;

import java.net.URI;
import java.util.UUID;

import com.fluxisai.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/job-opportunities")
public class JobOpportunityController {

    private final JobOpportunityService jobOpportunityService;

    public JobOpportunityController(JobOpportunityService jobOpportunityService) {
        this.jobOpportunityService = jobOpportunityService;
    }

    @PostMapping
    public ResponseEntity<JobOpportunityResponse> create(
            @Valid @RequestBody CreateJobOpportunityRequest request) {
        JobOpportunityResponse created = jobOpportunityService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public JobOpportunityResponse get(@PathVariable UUID id) {
        return jobOpportunityService.get(id);
    }

    @GetMapping
    public PageResponse<JobOpportunityResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return jobOpportunityService.list(pageable);
    }
}
