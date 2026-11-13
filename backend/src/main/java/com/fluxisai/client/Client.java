package com.fluxisai.client;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "clients")
public class Client {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "company_id") private UUID companyId;
    @Column(name = "contact_id") private UUID contactId;
    @Column(name = "display_name", nullable = false, length = 200) private String displayName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ClientStatus status;
    @Column(length = 100) private String origin;
    @Column(columnDefinition = "text") private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    protected Client() {}
    public Client(UUID userId, UUID companyId, UUID contactId, String displayName, ClientStatus status, String origin, String notes) { this.userId=userId; this.companyId=companyId; this.contactId=contactId; this.displayName=displayName; this.status=status==null?ClientStatus.POTENCIAL:status; this.origin=origin; this.notes=notes; }
    public void update(UUID companyId, UUID contactId, String displayName, ClientStatus status, String origin, String notes) { this.companyId=companyId; this.contactId=contactId; this.displayName=displayName; this.status=status==null?ClientStatus.POTENCIAL:status; this.origin=origin; this.notes=notes; }
    @PrePersist void onCreate(){var now=OffsetDateTime.now(ZoneOffset.UTC);createdAt=now;updatedAt=now;} @PreUpdate void onUpdate(){updatedAt=OffsetDateTime.now(ZoneOffset.UTC);}
    public UUID getId(){return id;} public UUID getUserId(){return userId;} public UUID getCompanyId(){return companyId;} public UUID getContactId(){return contactId;} public String getDisplayName(){return displayName;} public ClientStatus getStatus(){return status;} public String getOrigin(){return origin;} public String getNotes(){return notes;} public OffsetDateTime getCreatedAt(){return createdAt;} public OffsetDateTime getUpdatedAt(){return updatedAt;}
}
