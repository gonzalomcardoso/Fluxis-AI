package com.fluxisai.crm;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import jakarta.persistence.*;

@Entity @Table(name = "contacts")
public class Contact {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "company_id") private UUID companyId;
    @Column(name = "first_name", nullable = false, length = 100) private String firstName;
    @Column(name = "last_name", nullable = false, length = 100) private String lastName;
    @Column(length = 320) private String email;
    @Column(name = "job_title", length = 160) private String jobTitle;
    @Column(length = 100) private String origin;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    protected Contact() {}
    public Contact(UUID userId, UUID companyId, String firstName, String lastName, String email, String jobTitle, String origin, String notes) { this.userId=userId; this.companyId=companyId; this.firstName=firstName; this.lastName=lastName; this.email=email; this.jobTitle=jobTitle; this.origin=origin; this.notes=notes; }
    @PrePersist void onCreate() { var now=OffsetDateTime.now(ZoneOffset.UTC); createdAt=now; updatedAt=now; }
    @PreUpdate void onUpdate() { updatedAt=OffsetDateTime.now(ZoneOffset.UTC); }
    public void update(UUID companyId,String firstName,String lastName,String email,String jobTitle,String origin,String notes) { this.companyId=companyId; this.firstName=firstName; this.lastName=lastName; this.email=email; this.jobTitle=jobTitle; this.origin=origin; this.notes=notes; }
    public UUID getId(){return id;} public UUID getUserId(){return userId;} public UUID getCompanyId(){return companyId;} public String getFirstName(){return firstName;} public String getLastName(){return lastName;} public String getEmail(){return email;} public String getJobTitle(){return jobTitle;} public String getOrigin(){return origin;} public String getNotes(){return notes;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
