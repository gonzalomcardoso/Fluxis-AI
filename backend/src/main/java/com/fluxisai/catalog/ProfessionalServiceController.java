package com.fluxisai.catalog;

import java.net.URI;
import java.util.UUID;
import com.fluxisai.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController @RequestMapping("/api/services") public class ProfessionalServiceController {
    private final ProfessionalServiceService service; public ProfessionalServiceController(ProfessionalServiceService service){this.service=service;}
    @PostMapping public ResponseEntity<ProfessionalServiceResponse> create(@Valid @RequestBody ProfessionalServiceRequest r){var created=service.create(r);URI location=ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();return ResponseEntity.created(location).body(created);}
    @GetMapping public PageResponse<ProfessionalServiceResponse> list(@PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC) Pageable p){return service.list(p);}
    @GetMapping("/{id}") public ProfessionalServiceResponse get(@PathVariable UUID id){return service.get(id);}
    @PutMapping("/{id}") public ProfessionalServiceResponse update(@PathVariable UUID id,@Valid @RequestBody ProfessionalServiceRequest r){return service.update(id,r);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id){service.delete(id);}
}
