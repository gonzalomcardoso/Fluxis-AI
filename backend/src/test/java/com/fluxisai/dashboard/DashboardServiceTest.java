package com.fluxisai.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.fluxisai.catalog.ProfessionalServiceRepository;
import com.fluxisai.client.ClientRepository;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.proposal.Proposal;
import com.fluxisai.proposal.ProposalRepository;
import com.fluxisai.proposal.ProposalStatus;
import com.fluxisai.subscription.Subscription;
import com.fluxisai.subscription.SubscriptionPeriodicity;
import com.fluxisai.subscription.SubscriptionRepository;
import com.fluxisai.subscription.SubscriptionStatus;
import com.fluxisai.task.TaskRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class DashboardServiceTest {
    @Test
    void convertsYearlySubscriptionToMonthlyAndAnnual() {
        var subscription = new Subscription(UUID.randomUUID(), "Tool", "Provider", new BigDecimal("120.00"), "EUR",
                SubscriptionPeriodicity.YEARLY, null, null, SubscriptionStatus.ACTIVE, null, null, null);
        assertEquals(0, new BigDecimal("10.00").compareTo(DashboardService.monthly(subscription)));
        assertEquals(0, new BigDecimal("120.00").compareTo(DashboardService.annual(subscription)));
    }

    @Test
    void keepsDashboardMarginSeparatedByCurrency() {
        var userId = UUID.randomUUID();
        var currentUser = Mockito.mock(CurrentUser.class);
        when(currentUser.id()).thenReturn(userId);
        var clients = Mockito.mock(ClientRepository.class);
        var services = Mockito.mock(ProfessionalServiceRepository.class);
        var proposals = Mockito.mock(ProposalRepository.class);
        var subscriptions = Mockito.mock(SubscriptionRepository.class);
        var tasks = Mockito.mock(TaskRepository.class);
        when(clients.findAllByUserId(userId, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of()));
        when(services.findAllByUserId(userId, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of()));
        var proposal = new Proposal(userId, UUID.randomUUID(), "Accepted", null, "EUR", null, ProposalStatus.ACCEPTED);
        proposal.totals(new BigDecimal("100.00"));
        when(proposals.findAllByUserId(userId, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(proposal)));
        var subscription = new Subscription(userId, "Hosting", "Provider", new BigDecimal("9.22"), "EUR",
                SubscriptionPeriodicity.MONTHLY, null, null, SubscriptionStatus.ACTIVE, null, null, null);
        when(subscriptions.findAllByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).thenReturn(List.of(subscription));
        when(tasks.findAllByUserId(userId, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of()));

        var response = new DashboardService(clients, services, proposals, subscriptions, tasks, currentUser).get();
        var margin = response.margins().getFirst();
        assertEquals("EUR", margin.currency());
        assertEquals(0, new BigDecimal("90.78").compareTo(margin.margin()));
    }
}
