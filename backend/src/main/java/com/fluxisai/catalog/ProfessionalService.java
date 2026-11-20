package com.fluxisai.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
@Table(name = "services")
public class ProfessionalService {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 120)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model", nullable = false, length = 20)
    private PricingModel pricingModel;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal rate;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private boolean available;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProfessionalServiceStatus status;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ProfessionalService() {
    }

    public ProfessionalService(
            UUID userId,
            String name,
            String description,
            String category,
            PricingModel pricingModel,
            BigDecimal rate,
            String currency,
            Boolean available,
            ProfessionalServiceStatus status,
            String notes) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.pricingModel = pricingModel;
        this.rate = money(rate);
        this.currency = currency;
        this.available = available == null || available;
        this.status = status == null ? ProfessionalServiceStatus.ACTIVE : status;
        this.notes = notes;
    }

    public void update(
            String name,
            String description,
            String category,
            PricingModel pricingModel,
            BigDecimal rate,
            String currency,
            Boolean available,
            ProfessionalServiceStatus status,
            String notes) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.pricingModel = pricingModel;
        this.rate = money(rate);
        this.currency = currency;
        this.available = available == null || available;
        this.status = status == null ? ProfessionalServiceStatus.ACTIVE : status;
        this.notes = notes;
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

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public PricingModel getPricingModel() { return pricingModel; }
    public BigDecimal getRate() { return rate; }
    public String getCurrency() { return currency; }
    public boolean isAvailable() { return available; }
    public ProfessionalServiceStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}