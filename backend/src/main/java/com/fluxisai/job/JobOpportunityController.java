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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/job-opportunities")
@SecurityRequirement(name = "sessionCookie")
public class JobOpportunityController {

    private final JobOpportunityService jobOpportunityService;

    public JobOpportunityController(JobOpportunityService jobOpportunityService) {
        this.jobOpportunityService = jobOpportunityService;
    }

    @PostMapping
    @Operation(
            summary = "Create a job opportunity",
            description = "Creates an opportunity for the authenticated user.",
            parameters = @Parameter(
                    name = "X-CSRF-TOKEN",
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "Token returned by GET /api/auth/csrf."))
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
    @Operation(summary = "Get one of the authenticated user's opportunities")
    public JobOpportunityResponse get(@PathVariable UUID id) {
        return jobOpportunityService.get(id);
    }

    @GetMapping
    @Operation(summary = "List the authenticated user's opportunities")
    public PageResponse<JobOpportunityResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return jobOpportunityService.list(pageable);
    }
}
