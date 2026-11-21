package com.fluxisai.dashboard;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        OffsetDateTime generatedAt,
        ClientMetrics clients,
        ServiceMetrics services,
        ProposalMetrics proposals,
        SubscriptionMetrics subscriptions,
        TaskMetrics tasks,
        List<MarginMetric> margins) {
    public record ClientMetrics(long total, long potential, long active) {}
    public record ServiceMetrics(long total, long active) {}
    public record ProposalMetrics(long total, Map<String, Long> byStatus, List<MoneyMetric> acceptedByCurrency) {}
    public record SubscriptionMetrics(long active, List<MoneyMetric> monthlyByCurrency, List<MoneyMetric> annualByCurrency, long upcomingRenewals) {}
    public record TaskMetrics(long total, long pending, long inProgress, long done, long overdue) {}
    public record MoneyMetric(String currency, BigDecimal amount) {}
    public record MarginMetric(String currency, BigDecimal acceptedRevenue, BigDecimal monthlySubscriptionCost, BigDecimal margin) {}
}
