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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "connected_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_connected_accounts_user_provider",
                columnNames = {"user_id", "provider"}))
public class ConnectedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConnectedAccountProvider provider;

    @Column(name = "provider_account_id", nullable = false, length = 255)
    private String providerAccountId;

    @Column(name = "account_email", nullable = false, length = 320)
    private String accountEmail;

    @Column(name = "access_token_encrypted", columnDefinition = "text")
    private String accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted", columnDefinition = "text")
    private String refreshTokenEncrypted;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "granted_scopes", nullable = false, columnDefinition = "text[]")
    private List<String> grantedScopes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConnectedAccountStatus status;

    @Column(name = "encryption_key_version", nullable = false)
    private int encryptionKeyVersion;

    @Column(name = "connected_at", nullable = false)
    private OffsetDateTime connectedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected ConnectedAccount() {
    }

    public ConnectedAccount(
            UUID userId,
            ConnectedAccountProvider provider,
            String providerAccountId,
            String accountEmail,
            String accessTokenEncrypted,
            String refreshTokenEncrypted,
            OffsetDateTime tokenExpiresAt,
            List<String> grantedScopes,
            int encryptionKeyVersion) {
        this.userId = userId;
        this.provider = provider;
        this.providerAccountId = providerAccountId;
        this.accountEmail = accountEmail;
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.refreshTokenEncrypted = refreshTokenEncrypted;
        this.tokenExpiresAt = tokenExpiresAt;
        this.grantedScopes = grantedScopes == null ? List.of() : List.copyOf(grantedScopes);
        this.status = ConnectedAccountStatus.CONNECTED;
        this.encryptionKeyVersion = encryptionKeyVersion;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.connectedAt = now;
        this.updatedAt = now;
    }

    public void updateConnection(
            String accountEmail,
            String accessTokenEncrypted,
            String refreshTokenEncrypted,
            OffsetDateTime tokenExpiresAt,
            List<String> grantedScopes,
            int encryptionKeyVersion) {
        this.accountEmail = accountEmail;
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.refreshTokenEncrypted = refreshTokenEncrypted;
        this.tokenExpiresAt = tokenExpiresAt;
        this.grantedScopes = grantedScopes == null ? List.of() : List.copyOf(grantedScopes);
        this.encryptionKeyVersion = encryptionKeyVersion;
        this.status = ConnectedAccountStatus.CONNECTED;
        this.connectedAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.revokedAt = null;
    }

    public void revoke() {
        this.status = ConnectedAccountStatus.REVOKED;
        this.accessTokenEncrypted = null;
        this.refreshTokenEncrypted = null;
        this.tokenExpiresAt = null;
        this.revokedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (connectedAt == null) {
            connectedAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
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

    public String getProviderAccountId() {
        return providerAccountId;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    String getAccessTokenEncrypted() {
        return accessTokenEncrypted;
    }

    String getRefreshTokenEncrypted() {
        return refreshTokenEncrypted;
    }

    public OffsetDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public List<String> getGrantedScopes() {
        return grantedScopes;
    }

    public ConnectedAccountStatus getStatus() {
        return status;
    }

    public int getEncryptionKeyVersion() {
        return encryptionKeyVersion;
    }

    public OffsetDateTime getConnectedAt() {
        return connectedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }
}
