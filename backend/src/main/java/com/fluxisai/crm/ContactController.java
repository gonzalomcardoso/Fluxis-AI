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

@RestController @RequestMapping("/api/contacts") public class ContactController {
    private final ContactService service; public ContactController(ContactService service){this.service=service;}
    @PostMapping public ResponseEntity<ContactResponse> create(@Valid @RequestBody ContactRequest r){var c=service.create(r); URI location=ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(c.id()).toUri(); return ResponseEntity.created(location).body(c);}
    @GetMapping public PageResponse<ContactResponse> list(@PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC) Pageable p){return service.list(p);}
    @GetMapping("/{id}") public ContactResponse get(@PathVariable UUID id){return service.get(id);}
    @PutMapping("/{id}") public ContactResponse update(@PathVariable UUID id,@Valid @RequestBody ContactRequest r){return service.update(id,r);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id){service.delete(id);}
}
