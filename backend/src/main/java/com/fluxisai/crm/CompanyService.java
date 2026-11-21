package com.fluxisai.crm;

import java.util.UUID;
import com.fluxisai.common.error.*;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.common.web.PageResponse;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {
    private final CompanyRepository repository; private final CurrentUser currentUser;
    public CompanyService(CompanyRepository repository, CurrentUser currentUser) { this.repository = repository; this.currentUser = currentUser; }
    @Transactional public CompanyResponse create(CompanyRequest r) { var user = currentUser.id(); var name = r.name().trim(); if (repository.existsByUserIdAndNameIgnoreCase(user, name)) throw new ConflictException("Ya existe una empresa con ese nombre"); return CompanyResponse.from(repository.save(new Company(user, name, optional(r.website()), optional(r.industry()), optional(r.notes())))); }
    @Transactional(readOnly = true) public CompanyResponse get(UUID id) { return CompanyResponse.from(find(id)); }
    @Transactional(readOnly = true) public PageResponse<CompanyResponse> list(Pageable p) { return PageResponse.from(repository.findAllByUserId(currentUser.id(), p).map(CompanyResponse::from)); }
    @Transactional public CompanyResponse update(UUID id, CompanyRequest r) { var c = find(id); var name = r.name().trim(); if (repository.existsByUserIdAndNameIgnoreCaseAndIdNot(currentUser.id(), name, id)) throw new ConflictException("Ya existe una empresa con ese nombre"); c.update(name, optional(r.website()), optional(r.industry()), optional(r.notes())); return CompanyResponse.from(repository.save(c)); }
    @Transactional public void delete(UUID id) { repository.delete(find(id)); }
    private Company find(UUID id) { return repository.findByIdAndUserId(id, currentUser.id()).orElseThrow(() -> new ResourceNotFoundException("No se encontró la empresa")); }
    private static String optional(String value) { if (value == null) return null; var v = value.trim(); return v.isEmpty() ? null : v; }
}
