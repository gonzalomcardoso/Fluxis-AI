package com.fluxisai.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.UUID;
import java.util.Optional;
import com.fluxisai.common.error.InvalidRequestException;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.crm.Company;
import com.fluxisai.crm.CompanyRepository;
import com.fluxisai.crm.Contact;
import com.fluxisai.crm.ContactRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class) class ClientServiceTest {
    static final UUID USER=UUID.fromString("11111111-1111-1111-1111-111111111111"); static final UUID COMPANY=UUID.fromString("22222222-2222-2222-2222-222222222222"); static final UUID CONTACT=UUID.fromString("33333333-3333-3333-3333-333333333333");
    @Mock ClientRepository clients; @Mock CompanyRepository companies; @Mock ContactRepository contacts; @Mock CurrentUser currentUser; @InjectMocks ClientService service;
    @Test void createsClientWithOwnedRelations(){when(currentUser.id()).thenReturn(USER);when(companies.findByIdAndUserId(COMPANY,USER)).thenReturn(Optional.of(new Company(USER,"Acme",null,null,null)));when(contacts.findByIdAndUserId(CONTACT,USER)).thenReturn(Optional.of(new Contact(USER,COMPANY,"Ada","Lovelace",null,null,null,null)));when(clients.save(any())).thenAnswer(i->i.getArgument(0));var result=service.create(new ClientRequest(COMPANY,CONTACT,"  Acme Client  ",null,null,null));assertEquals("Acme Client",result.displayName());verify(clients).save(any(Client.class));}
    @Test void rejectsContactFromAnotherCompany(){when(currentUser.id()).thenReturn(USER);when(companies.findByIdAndUserId(COMPANY,USER)).thenReturn(Optional.of(new Company(USER,"Acme",null,null,null)));when(contacts.findByIdAndUserId(CONTACT,USER)).thenReturn(Optional.of(new Contact(USER,UUID.randomUUID(),"Ada","Lovelace",null,null,null,null)));assertThrows(InvalidRequestException.class,()->service.create(new ClientRequest(COMPANY,CONTACT,"Client",null,null,null)));verify(clients,never()).save(any());}
}
