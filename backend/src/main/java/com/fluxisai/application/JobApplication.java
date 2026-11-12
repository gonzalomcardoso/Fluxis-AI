package com.fluxisai.application;

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
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "job_opportunity_id", nullable = false)
    private UUID jobOpportunityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "applied_on")
    private LocalDate appliedOn;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected JobApplication() {
    }

    public JobApplication(UUID userId, UUID jobOpportunityId, ApplicationStatus status, LocalDate appliedOn, String notes) {
        this.userId = userId;
        this.jobOpportunityId = jobOpportunityId;
        this.status = status == null ? ApplicationStatus.PENDING : status;
        this.appliedOn = appliedOn;
        this.notes = notes;
    }

    public void update(ApplicationStatus status, LocalDate appliedOn, String notes) {
        this.status = status == null ? ApplicationStatus.PENDING : status;
        this.appliedOn = appliedOn;
        this.notes = notes;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getJobOpportunityId() { return jobOpportunityId; }
    public ApplicationStatus getStatus() { return status; }
    public LocalDate getAppliedOn() { return appliedOn; }
    public String getNotes() { return notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
