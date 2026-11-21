package com.fluxisai.proposal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.fluxisai.catalog.ProfessionalService;
import com.fluxisai.catalog.ProfessionalServiceRepository;
import com.fluxisai.client.ClientRepository;
import com.fluxisai.common.error.InvalidRequestException;
import com.fluxisai.common.error.ResourceNotFoundException;
import com.fluxisai.common.security.CurrentUser;
import com.fluxisai.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProposalService {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999999999999.99");

    private final ProposalRepository proposals;
    private final ProposalItemRepository items;
    private final ProposalHistoryRepository history;
    private final ClientRepository clients;
    private final ProfessionalServiceRepository services;
    private final CurrentUser currentUser;

    public ProposalService(
            ProposalRepository proposals,
            ProposalItemRepository items,
            ProposalHistoryRepository history,
            ClientRepository clients,
            ProfessionalServiceRepository services,
            CurrentUser currentUser) {
        this.proposals = proposals;
        this.items = items;
        this.history = history;
        this.clients = clients;
        this.services = services;
        this.currentUser = currentUser;
    }

    @Transactional
    public ProposalResponse create(ProposalRequest request) {
        validateClient(request.clientId());
        validateItems(request.items(), request.currency());
        Proposal proposal = new Proposal(
                currentUser.id(),
                request.clientId(),
                clean(request.title()),
                optional(request.description()),
                request.currency(),
                request.validUntil(),
                request.status());
        return replaceItems(proposal, request.items(), "CREATED");
    }

    @Transactional(readOnly = true)
    public ProposalResponse get(UUID id) {
        Proposal proposal = find(id);
        return ProposalResponse.from(proposal, items.findAllByProposalId(proposal.getId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProposalResponse> list(Pageable pageable) {
        return PageResponse.from(proposals.findAllByUserId(currentUser.id(), pageable)
                .map(proposal -> ProposalResponse.from(proposal, items.findAllByProposalId(proposal.getId()))));
    }

    @Transactional(readOnly = true)
    public List<ProposalHistoryResponse> history(UUID id) {
        find(id);
        return history.findAllByProposalIdAndUserIdOrderByChangedAtDesc(id, currentUser.id()).stream()
                .map(ProposalHistoryResponse::from)
                .toList();
    }

    @Transactional
    public ProposalResponse update(UUID id, ProposalRequest request) {
        Proposal proposal = find(id);
        validateClient(request.clientId());
        validateItems(request.items(), request.currency());
        proposal.update(
                request.clientId(),
                clean(request.title()),
                optional(request.description()),
                request.currency(),
                request.validUntil(),
                request.status());
        return replaceItems(proposal, request.items(), "UPDATED");
    }

    @Transactional
    public void delete(UUID id) {
        proposals.delete(find(id));
    }

    private ProposalResponse replaceItems(Proposal proposal, List<ProposalItemRequest> requests, String changeType) {
        Proposal saved = proposals.save(proposal);
        items.deleteAllByProposalId(saved.getId());

        List<ProposalItem> created = requests.stream()
                .map(item -> new ProposalItem(
                        saved.getId(),
                        item.serviceId(),
                        clean(item.description()),
                        item.quantity(),
                        item.unitPrice()))
                .toList();
        created.forEach(item -> validateAmount(item.getAmount()));

        items.saveAll(created);
        BigDecimal total = created.stream()
                .map(ProposalItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        validateAmount(total);
        saved.totals(total);
        proposals.save(saved);
        history.save(new ProposalHistory(
                saved.getId(),
                currentUser.id(),
                changeType,
                saved.getStatus(),
                saved.getSubtotal(),
                saved.getTotal()));
        return ProposalResponse.from(saved, created);
    }

    private void validateClient(UUID id) {
        if (clients.findByIdAndUserId(id, currentUser.id()).isEmpty()) {
            throw new ResourceNotFoundException("El cliente indicado no existe");
        }
    }

    private void validateItems(List<ProposalItemRequest> requests, String proposalCurrency) {
        String normalizedCurrency = proposalCurrency.trim().toUpperCase(Locale.ROOT);
        for (ProposalItemRequest request : requests) {
            ProfessionalService service = services.findByIdAndUserId(request.serviceId(), currentUser.id())
                    .orElseThrow(() -> new ResourceNotFoundException("El servicio indicado no existe"));
            if (!service.getCurrency().equalsIgnoreCase(normalizedCurrency)) {
                throw new InvalidRequestException(
                        "La moneda del servicio " + service.getName() + " no coincide con la moneda de la propuesta");
            }
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidRequestException("El importe de la propuesta supera el máximo permitido");
        }
    }

    private Proposal find(UUID id) {
        return proposals.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la propuesta"));
    }

    private static String clean(String value) {
        return value.trim();
    }

    private static String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}