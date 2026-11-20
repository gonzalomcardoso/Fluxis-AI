package com.fluxisai.proposal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "proposals")
public class Proposal {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProposalStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Proposal() {
    }

    public Proposal(
            UUID userId,
            UUID clientId,
            String title,
            String description,
            String currency,
            LocalDate validUntil,
            ProposalStatus status) {
        this.userId = userId;
        this.clientId = clientId;
        this.title = title;
        this.description = description;
        this.currency = currency;
        this.validUntil = validUntil;
        this.status = status == null ? ProposalStatus.DRAFT : status;
        this.subtotal = BigDecimal.ZERO.setScale(2);
        this.total = BigDecimal.ZERO.setScale(2);
    }

    public void update(
            UUID clientId,
            String title,
            String description,
            String currency,
            LocalDate validUntil,
            ProposalStatus status) {
        this.clientId = clientId;
        this.title = title;
        this.description = description;
        this.currency = currency;
        this.validUntil = validUntil;
        this.status = status == null ? ProposalStatus.DRAFT : status;
    }

    public void totals(BigDecimal total) {
        this.subtotal = total;
        this.total = total;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getClientId() { return clientId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCurrency() { return currency; }
    public LocalDate getValidUntil() { return validUntil; }
    public ProposalStatus getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTotal() { return total; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}