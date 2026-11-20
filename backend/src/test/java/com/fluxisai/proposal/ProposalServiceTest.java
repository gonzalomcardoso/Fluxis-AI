package com.fluxisai.proposal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fluxisai.catalog.ProfessionalService;
import com.fluxisai.catalog.ProfessionalServiceRepository;
import com.fluxisai.catalog.ProfessionalServiceStatus;
import com.fluxisai.catalog.PricingModel;
import com.fluxisai.client.Client;
import com.fluxisai.client.ClientRepository;
import com.fluxisai.client.ClientStatus;
import com.fluxisai.common.error.InvalidRequestException;
import com.fluxisai.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SERVICE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private ProposalRepository proposals;

    @Mock
    private ProposalItemRepository items;

    @Mock
    private ProposalHistoryRepository history;

    @Mock
    private ClientRepository clients;

    @Mock
    private ProfessionalServiceRepository services;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private ProposalService service;

    @BeforeEach
    void setUp() {
        when(currentUser.id()).thenReturn(USER_ID);
        when(clients.findByIdAndUserId(CLIENT_ID, USER_ID))
                .thenReturn(Optional.of(new Client(USER_ID, null, null, "Client", ClientStatus.POTENCIAL, null, null)));
        lenient().when(proposals.save(any(Proposal.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectsServiceWhoseCurrencyDiffersFromProposalCurrency() {
        when(services.findByIdAndUserId(SERVICE_ID, USER_ID)).thenReturn(Optional.of(service("USD")));

        ProposalRequest request = request("EUR");

        assertThrows(InvalidRequestException.class, () -> service.create(request));
        verify(proposals, never()).save(any(Proposal.class));
        verify(items, never()).saveAll(any());
    }

    @Test
    void roundsLineAmountsAndTotalToTwoDecimalPlaces() {
        when(services.findByIdAndUserId(SERVICE_ID, USER_ID)).thenReturn(Optional.of(service("EUR")));

        ProposalResponse response = service.create(request("EUR"));

        assertEquals(new BigDecimal("2.35"), response.items().getFirst().unitPrice());
        assertEquals(new BigDecimal("2.89"), response.items().getFirst().amount());
        assertEquals(new BigDecimal("2.89"), response.total());
        assertEquals(2, response.total().scale());
    }

    private static ProposalRequest request(String currency) {
        return new ProposalRequest(
                CLIENT_ID,
                "Proposal",
                null,
                currency,
                null,
                ProposalStatus.DRAFT,
                List.of(new ProposalItemRequest(
                        SERVICE_ID,
                        "Consulting",
                        new BigDecimal("1.23"),
                        new BigDecimal("2.35"))));
    }

    private static ProfessionalService service(String currency) {
        return new ProfessionalService(
                USER_ID,
                "Consulting",
                null,
                null,
                PricingModel.HOURLY,
                new BigDecimal("2.35"),
                currency,
                true,
                ProfessionalServiceStatus.ACTIVE,
                null);
    }
}