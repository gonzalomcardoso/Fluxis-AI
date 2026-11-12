package com.fluxisai.application;

import java.net.URI;
import java.util.UUID;

import com.fluxisai.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/job-applications")
public class JobApplicationController {
    private final JobApplicationService service;
    public JobApplicationController(JobApplicationService service) { this.service = service; }
    @PostMapping public ResponseEntity<JobApplicationResponse> create(@Valid @RequestBody CreateJobApplicationRequest request) { JobApplicationResponse created=service.create(request); URI location=ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri(); return ResponseEntity.created(location).body(created); }
    @GetMapping public PageResponse<JobApplicationResponse> list(@PageableDefault(size=20, sort="createdAt", direction=Sort.Direction.DESC) Pageable pageable) { return service.list(pageable); }
    @GetMapping("/{id}") public JobApplicationResponse get(@PathVariable UUID id) { return service.get(id); }
    @PutMapping("/{id}") public JobApplicationResponse update(@PathVariable UUID id,@Valid @RequestBody UpdateJobApplicationRequest request){return service.update(id,request);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id){service.delete(id);}
}
