package com.fluxisai.application;

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

@Entity
@Table(name = "application_interviews")
public class ApplicationInterview {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "job_application_id", nullable = false) private UUID jobApplicationId;
    @Column(name = "scheduled_at", nullable = false) private OffsetDateTime scheduledAt;
    @Column(name = "interview_type", nullable = false, length = 100) private String interviewType;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    protected ApplicationInterview() {}
    public ApplicationInterview(UUID userId, UUID jobApplicationId, OffsetDateTime scheduledAt, String interviewType, String notes) { this.userId=userId; this.jobApplicationId=jobApplicationId; this.scheduledAt=scheduledAt; this.interviewType=interviewType; this.notes=notes; }
    public void update(OffsetDateTime scheduledAt, String interviewType, String notes) { this.scheduledAt=scheduledAt; this.interviewType=interviewType; this.notes=notes; }
    @PrePersist void onCreate(){var now=OffsetDateTime.now(ZoneOffset.UTC);createdAt=now;updatedAt=now;} @PreUpdate void onUpdate(){updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
    public UUID getId(){return id;} public UUID getUserId(){return userId;} public UUID getJobApplicationId(){return jobApplicationId;} public OffsetDateTime getScheduledAt(){return scheduledAt;} public String getInterviewType(){return interviewType;} public String getNotes(){return notes;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
