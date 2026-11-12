package com.fluxisai.application;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController @RequestMapping("/api/job-applications/{applicationId}/interviews")
public class ApplicationInterviewController {
    private final ApplicationInterviewService service; public ApplicationInterviewController(ApplicationInterviewService service){this.service=service;}
    @GetMapping public List<InterviewResponse> list(@PathVariable UUID applicationId){return service.list(applicationId);}
    @PostMapping public ResponseEntity<InterviewResponse> create(@PathVariable UUID applicationId,@Valid @RequestBody InterviewRequest request){var created=service.create(applicationId,request); URI location=ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();return ResponseEntity.created(location).body(created);}
    @PutMapping("/{id}") public InterviewResponse update(@PathVariable UUID applicationId,@PathVariable UUID id,@Valid @RequestBody InterviewRequest request){return service.update(applicationId,id,request);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID applicationId,@PathVariable UUID id){service.delete(applicationId,id);}
}
