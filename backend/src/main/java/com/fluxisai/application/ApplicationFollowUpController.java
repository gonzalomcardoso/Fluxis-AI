package com.fluxisai.application;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController @RequestMapping("/api/job-applications/{applicationId}/follow-ups")
public class ApplicationFollowUpController {
    private final ApplicationFollowUpService service; public ApplicationFollowUpController(ApplicationFollowUpService service){this.service=service;}
    @GetMapping public List<FollowUpResponse> list(@PathVariable UUID applicationId){return service.list(applicationId);}
    @PostMapping public ResponseEntity<FollowUpResponse> create(@PathVariable UUID applicationId,@Valid @RequestBody FollowUpRequest request){var created=service.create(applicationId,request);URI location=ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();return ResponseEntity.created(location).body(created);}
    @PutMapping("/{id}") public FollowUpResponse update(@PathVariable UUID applicationId,@PathVariable UUID id,@Valid @RequestBody FollowUpRequest request){return service.update(applicationId,id,request);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID applicationId,@PathVariable UUID id){service.delete(applicationId,id);}
}
