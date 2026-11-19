package com.fluxisai.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.fluxisai.common.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    @Mock
    private SubscriptionRepository subscriptions;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private SubscriptionService service;

    @Test
    void monthlyAndAnnualKeepCurrencyAmountsSeparate() {
        var user = UUID.randomUUID();
        var monthly = new Subscription(user, "Hosting", "Cloud", new BigDecimal("9.22"), "EUR",
                SubscriptionPeriodicity.MONTHLY, null, null, SubscriptionStatus.ACTIVE, null, null, null);
        var yearly = new Subscription(user, "Tool", "Vendor", new BigDecimal("120.00"), "USD",
                SubscriptionPeriodicity.YEARLY, null, null, SubscriptionStatus.ACTIVE, null, null, null);

        assertEquals(0, new BigDecimal("9.22").compareTo(SubscriptionService.monthly(monthly)));
        assertEquals(0, new BigDecimal("110.64").compareTo(SubscriptionService.annual(monthly)));
        assertEquals(0, new BigDecimal("10.00").compareTo(SubscriptionService.monthly(yearly)));
        assertEquals(0, new BigDecimal("120.00").compareTo(SubscriptionService.annual(yearly)));
    }

    @Test
    void summaryIncludesOnlyRenewalsBetweenTodayAndThirtyDaysAhead() {
        var user = UUID.randomUUID();
        var today = LocalDate.now(ZoneOffset.UTC);
        var past = new Subscription(user, "Past", "Provider", new BigDecimal("10.00"), "EUR",
                SubscriptionPeriodicity.MONTHLY, null, today.minusDays(1), SubscriptionStatus.ACTIVE, null, null, null);
        var upcoming = new Subscription(user, "Upcoming", "Provider", new BigDecimal("20.00"), "EUR",
                SubscriptionPeriodicity.MONTHLY, null, today.plusDays(10), SubscriptionStatus.ACTIVE, null, null, null);
        var tooFar = new Subscription(user, "Later", "Provider", new BigDecimal("30.00"), "EUR",
                SubscriptionPeriodicity.MONTHLY, null, today.plusDays(31), SubscriptionStatus.ACTIVE, null, null, null);

        when(currentUser.id()).thenReturn(user);
        when(subscriptions.findAllByUserIdAndStatus(user, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(past, upcoming, tooFar));

        var response = service.summary();

        assertEquals(1, response.upcomingRenewals().size());
        assertEquals("Upcoming", response.upcomingRenewals().getFirst().name());
    }
}