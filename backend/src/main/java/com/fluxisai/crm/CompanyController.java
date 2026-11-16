package com.fluxisai.crm;

import java.net.URI;
import java.util.UUID;
import com.fluxisai.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController @RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyService service;
    public CompanyController(CompanyService service) { this.service = service; }
    @PostMapping public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CompanyRequest r) { var created = service.create(r); URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri(); return ResponseEntity.created(location).body(created); }
    @GetMapping public PageResponse<CompanyResponse> list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) { return service.list(p); }
    @GetMapping("/{id}") public CompanyResponse get(@PathVariable UUID id) { return service.get(id); }
    @PutMapping("/{id}") public CompanyResponse update(@PathVariable UUID id, @Valid @RequestBody CompanyRequest r) { return service.update(id, r); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { service.delete(id); }
}
