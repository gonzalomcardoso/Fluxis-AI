package com.fluxisai.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity @Table(name = "application_follow_ups")
public class ApplicationFollowUp {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "job_application_id", nullable = false) private UUID jobApplicationId;
    @Column(name = "due_on", nullable = false) private LocalDate dueOn;
    @Column(nullable = false, length = 500) private String description;
    @Column(nullable = false) private boolean completed;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    protected ApplicationFollowUp() {}
    public ApplicationFollowUp(UUID userId, UUID jobApplicationId, LocalDate dueOn, String description, boolean completed) { this.userId=userId; this.jobApplicationId=jobApplicationId; this.dueOn=dueOn; this.description=description; this.completed=completed; }
    public void update(LocalDate dueOn,String description,boolean completed){this.dueOn=dueOn;this.description=description;this.completed=completed;}
    @PrePersist void onCreate(){var now=OffsetDateTime.now(ZoneOffset.UTC);createdAt=now;updatedAt=now;} @PreUpdate void onUpdate(){updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
    public UUID getId(){return id;} public UUID getUserId(){return userId;} public UUID getJobApplicationId(){return jobApplicationId;} public LocalDate getDueOn(){return dueOn;} public String getDescription(){return description;} public boolean isCompleted(){return completed;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
