package com.fluxisai.gmail;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "oauth_authorization_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_oauth_authorization_requests_state_hash",
                columnNames = "state_hash"))
public class OAuthAuthorizationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConnectedAccountProvider provider;

    @Column(name = "state_hash", nullable = false, length = 64)
    private String stateHash;

    @Column(name = "code_verifier_encrypted", nullable = false, columnDefinition = "text")
    private String codeVerifierEncrypted;

    @Column(name = "encryption_key_version", nullable = false)
    private int encryptionKeyVersion;

    /**
     * Requested OAuth scopes. The contract is that this list is never null and
     * is exposed as an immutable copy.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "requested_scopes", nullable = false, columnDefinition = "text[]")
    private List<String> requestedScopes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    protected OAuthAuthorizationRequest() {
    }

    public OAuthAuthorizationRequest(
            UUID userId,
            ConnectedAccountProvider provider,
            String stateHash,
            String codeVerifierEncrypted,
            int encryptionKeyVersion,
            List<String> requestedScopes,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt) {
        this.userId = userId;
        this.provider = provider;
        this.stateHash = stateHash;
        this.codeVerifierEncrypted = codeVerifierEncrypted;
        this.encryptionKeyVersion = encryptionKeyVersion;
        this.requestedScopes = requestedScopes == null ? List.of() : List.copyOf(requestedScopes);
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    @Override
    public String toString() {
        return "OAuthAuthorizationRequest[id=%s, userId=%s, provider=%s, expiresAt=%s]"
                .formatted(id, userId, provider, expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public ConnectedAccountProvider getProvider() {
        return provider;
    }

    public String getStateHash() {
        return stateHash;
    }

    public String getCodeVerifierEncrypted() {
        return codeVerifierEncrypted;
    }

    public int getEncryptionKeyVersion() {
        return encryptionKeyVersion;
    }

    public List<String> getRequestedScopes() {
        return requestedScopes == null ? List.of() : List.copyOf(requestedScopes);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }
}