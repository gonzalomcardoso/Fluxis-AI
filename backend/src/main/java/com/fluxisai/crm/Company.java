package com.fluxisai.crm;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, length = 200) private String name;
    @Column(length = 2048) private String website;
    @Column(length = 120) private String industry;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    protected Company() {}
    public Company(UUID userId, String name, String website, String industry, String notes) {
        this.userId = userId; this.name = name; this.website = website; this.industry = industry; this.notes = notes;
    }
    @PrePersist void onCreate() { var now = OffsetDateTime.now(ZoneOffset.UTC); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(ZoneOffset.UTC); }
    public void update(String name, String website, String industry, String notes) { this.name = name; this.website = website; this.industry = industry; this.notes = notes; }
    public UUID getId() { return id; } public UUID getUserId() { return userId; } public String getName() { return name; }
    public String getWebsite() { return website; } public String getIndustry() { return industry; } public String getNotes() { return notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; } public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
