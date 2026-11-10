package com.fluxisai.crm;

import java.util.UUID;
import com.fluxisai.common.error.*;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.common.web.PageResponse;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class ContactService {
    private final ContactRepository repository; private final CompanyRepository companies; private final CurrentUser currentUser;
    public ContactService(ContactRepository repository, CompanyRepository companies, CurrentUser currentUser) { this.repository=repository; this.companies=companies; this.currentUser=currentUser; }
    @Transactional public ContactResponse create(ContactRequest r) { var user=currentUser.id(); var email=optional(r.email()); checkCompany(r.companyId()); checkDuplicate(user,email,null); return ContactResponse.from(repository.save(new Contact(user,r.companyId(),clean(r.firstName()),clean(r.lastName()),email,optional(r.jobTitle()),optional(r.origin()),optional(r.notes())))); }
    @Transactional(readOnly=true) public ContactResponse get(UUID id) { return ContactResponse.from(find(id)); }
    @Transactional(readOnly=true) public PageResponse<ContactResponse> list(Pageable p) { return PageResponse.from(repository.findAllByUserId(currentUser.id(),p).map(ContactResponse::from)); }
    @Transactional public ContactResponse update(UUID id, ContactRequest r) { var c=find(id); var email=optional(r.email()); checkCompany(r.companyId()); checkDuplicate(currentUser.id(),email,id); c.update(r.companyId(),clean(r.firstName()),clean(r.lastName()),email,optional(r.jobTitle()),optional(r.origin()),optional(r.notes())); return ContactResponse.from(repository.save(c)); }
    @Transactional public void delete(UUID id) { repository.delete(find(id)); }
    private Contact find(UUID id){return repository.findByIdAndUserId(id,currentUser.id()).orElseThrow(() -> new ResourceNotFoundException("No se encontró el contacto"));}
    private void checkCompany(UUID id){if(id != null && companies.findByIdAndUserId(id,currentUser.id()).isEmpty()) throw new ResourceNotFoundException("La empresa indicada no existe");}
    private void checkDuplicate(UUID user,String email,UUID id){if(email != null && (id == null ? repository.existsByUserIdAndEmailIgnoreCase(user,email) : repository.existsByUserIdAndEmailIgnoreCaseAndIdNot(user,email,id))) throw new ConflictException("Ya existe un contacto con ese correo");}
    private static String clean(String value){return value.trim();} private static String optional(String value){if(value==null)return null;var v=value.trim();return v.isEmpty()?null:v;}
}
