package com.fluxisai.client;

import java.net.URI;
import java.util.UUID;
import com.fluxisai.common.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController @RequestMapping("/api/clients")
public class ClientController {
    private final ClientService service; public ClientController(ClientService service){this.service=service;}
    @PostMapping public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest r){var created=service.create(r);URI location=ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri();return ResponseEntity.created(location).body(created);}
    @GetMapping public PageResponse<ClientResponse> list(@PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC) Pageable p){return service.list(p);}
    @GetMapping("/{id}") public ClientResponse get(@PathVariable UUID id){return service.get(id);}
    @PutMapping("/{id}") public ClientResponse update(@PathVariable UUID id,@Valid @RequestBody ClientRequest r){return service.update(id,r);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id){service.delete(id);}
}
