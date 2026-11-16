package com.fluxisai.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;

import com.fluxisai.catalog.ProfessionalServiceRepository;
import com.fluxisai.catalog.ProfessionalServiceStatus;
import com.fluxisai.client.ClientRepository;
import com.fluxisai.client.ClientStatus;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.proposal.ProposalRepository;
import com.fluxisai.proposal.ProposalStatus;
import com.fluxisai.subscription.Subscription;
import com.fluxisai.subscription.SubscriptionRepository;
import com.fluxisai.subscription.SubscriptionStatus;
import com.fluxisai.task.TaskRepository;
import com.fluxisai.task.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final ClientRepository clients;
    private final ProfessionalServiceRepository services;
    private final ProposalRepository proposals;
    private final SubscriptionRepository subscriptions;
    private final TaskRepository tasks;
    private final CurrentUser currentUser;

    public DashboardService(ClientRepository clients, ProfessionalServiceRepository services, ProposalRepository proposals,
                            SubscriptionRepository subscriptions, TaskRepository tasks, CurrentUser currentUser) {
        this.clients = clients;
        this.services = services;
        this.proposals = proposals;
        this.subscriptions = subscriptions;
        this.tasks = tasks;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public DashboardResponse get() {
        var userId = currentUser.id();
        var clientList = clients.findAllByUserId(userId, Pageable.unpaged()).getContent();
        var serviceList = services.findAllByUserId(userId, Pageable.unpaged()).getContent();
        var proposalList = proposals.findAllByUserId(userId, Pageable.unpaged()).getContent();
        var subscriptionList = subscriptions.findAllByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
        var taskList = tasks.findAllByUserId(userId, Pageable.unpaged()).getContent();

        var byStatus = new TreeMap<String, Long>();
        var accepted = new TreeMap<String, BigDecimal>();
        proposalList.forEach(p -> {
            byStatus.merge(p.getStatus().name(), 1L, Long::sum);
            if (p.getStatus() == ProposalStatus.ACCEPTED) {
                accepted.merge(p.getCurrency(), scale(p.getTotal()), BigDecimal::add);
            }
        });

        var monthly = new TreeMap<String, BigDecimal>();
        var annual = new TreeMap<String, BigDecimal>();
        var upcoming = subscriptionList.stream().filter(s -> s.getRenewalDate() != null
                && !s.getRenewalDate().isAfter(LocalDate.now().plusDays(30))).count();
        subscriptionList.forEach(s -> {
            monthly.merge(s.getCurrency(), scale(monthly(s)), BigDecimal::add);
            annual.merge(s.getCurrency(), scale(annual(s)), BigDecimal::add);
        });

        var margins = new TreeMap<String, BigDecimal>();
        accepted.forEach((currency, revenue) -> margins.put(currency, revenue.subtract(monthly.getOrDefault(currency, BigDecimal.ZERO))));
        monthly.forEach((currency, cost) -> margins.putIfAbsent(currency, BigDecimal.ZERO.subtract(cost)));

        var now = LocalDate.now();
        var pending = taskList.stream().filter(t -> t.getStatus() == TaskStatus.TODO).count();
        var inProgress = taskList.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
        var done = taskList.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        var overdue = taskList.stream().filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(now)
                && t.getStatus() != TaskStatus.DONE && t.getStatus() != TaskStatus.CANCELLED).count();

        return new DashboardResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                new DashboardResponse.ClientMetrics(clientList.size(), count(clientList, c -> c.getStatus() == ClientStatus.POTENCIAL), count(clientList, c -> c.getStatus() == ClientStatus.ACTIVE)),
                new DashboardResponse.ServiceMetrics(serviceList.size(), count(serviceList, s -> s.getStatus() == ProfessionalServiceStatus.ACTIVE)),
                new DashboardResponse.ProposalMetrics(proposalList.size(), byStatus, moneyList(accepted)),
                new DashboardResponse.SubscriptionMetrics(subscriptionList.size(), moneyList(monthly), moneyList(annual), upcoming),
                new DashboardResponse.TaskMetrics(taskList.size(), pending, inProgress, done, overdue),
                margins.entrySet().stream().map(e -> new DashboardResponse.MarginMetric(e.getKey(), scale(accepted.getOrDefault(e.getKey(), BigDecimal.ZERO)), scale(monthly.getOrDefault(e.getKey(), BigDecimal.ZERO)), scale(e.getValue()))).toList());
    }

    private static <T> long count(Iterable<T> values, java.util.function.Predicate<T> predicate) { long result = 0; for (var value : values) if (predicate.test(value)) result++; return result; }
    private static java.util.List<DashboardResponse.MoneyMetric> moneyList(Map<String, BigDecimal> values) { return values.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> new DashboardResponse.MoneyMetric(e.getKey(), scale(e.getValue()))).toList(); }
    static BigDecimal monthly(Subscription s) { return switch (s.getPeriodicity()) { case WEEKLY -> s.getAmount().multiply(BigDecimal.valueOf(52)).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP); case MONTHLY -> s.getAmount(); case QUARTERLY -> s.getAmount().divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP); case YEARLY -> s.getAmount().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP); }; }
    static BigDecimal annual(Subscription s) { return switch (s.getPeriodicity()) { case WEEKLY -> s.getAmount().multiply(BigDecimal.valueOf(52)); case MONTHLY -> s.getAmount().multiply(BigDecimal.valueOf(12)); case QUARTERLY -> s.getAmount().multiply(BigDecimal.valueOf(4)); case YEARLY -> s.getAmount(); }; }
    private static BigDecimal scale(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
