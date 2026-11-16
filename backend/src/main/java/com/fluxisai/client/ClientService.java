package com.fluxisai.client;

import java.util.UUID;
import com.fluxisai.common.error.InvalidRequestException;
import com.fluxisai.common.error.ResourceNotFoundException;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.common.web.PageResponse;
import com.fluxisai.crm.CompanyRepository;
import com.fluxisai.crm.Contact;
import com.fluxisai.crm.ContactRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {
    private final ClientRepository clients; private final CompanyRepository companies; private final ContactRepository contacts; private final CurrentUser currentUser;
    public ClientService(ClientRepository clients, CompanyRepository companies, ContactRepository contacts, CurrentUser currentUser){this.clients=clients;this.companies=companies;this.contacts=contacts;this.currentUser=currentUser;}
    @Transactional public ClientResponse create(ClientRequest r){validateRelations(r.companyId(),r.contactId()); return ClientResponse.from(clients.save(new Client(currentUser.id(),r.companyId(),r.contactId(),clean(r.displayName()),r.status(),optional(r.origin()),optional(r.notes()))));}
    @Transactional(readOnly=true) public ClientResponse get(UUID id){return ClientResponse.from(find(id));}
    @Transactional(readOnly=true) public PageResponse<ClientResponse> list(Pageable pageable){return PageResponse.from(clients.findAllByUserId(currentUser.id(),pageable).map(ClientResponse::from));}
    @Transactional public ClientResponse update(UUID id,ClientRequest r){var client=find(id);validateRelations(r.companyId(),r.contactId());client.update(r.companyId(),r.contactId(),clean(r.displayName()),r.status(),optional(r.origin()),optional(r.notes()));return ClientResponse.from(clients.save(client));}
    @Transactional public void delete(UUID id){clients.delete(find(id));}
    private Client find(UUID id){return clients.findByIdAndUserId(id,currentUser.id()).orElseThrow(()->new ResourceNotFoundException("No se encontró el cliente"));}
    private void validateRelations(UUID companyId,UUID contactId){var user=currentUser.id();if(companyId!=null&&companies.findByIdAndUserId(companyId,user).isEmpty())throw new ResourceNotFoundException("La empresa indicada no existe"); if(contactId!=null){Contact contact=contacts.findByIdAndUserId(contactId,user).orElseThrow(()->new ResourceNotFoundException("El contacto indicado no existe"));if(companyId!=null&&contact.getCompanyId()!=null&&!companyId.equals(contact.getCompanyId()))throw new InvalidRequestException("El contacto no pertenece a la empresa seleccionada");}}
    private static String clean(String value){return value.trim();} private static String optional(String value){if(value==null)return null;var v=value.trim();return v.isEmpty()?null:v;}
}
