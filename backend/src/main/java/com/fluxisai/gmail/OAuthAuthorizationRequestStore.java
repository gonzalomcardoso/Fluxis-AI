package com.fluxisai.gmail;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence boundary for pending OAuth authorization requests.
 */
@Service
public class OAuthAuthorizationRequestStore {

    private final OAuthAuthorizationRequestRepository requests;

    public OAuthAuthorizationRequestStore(OAuthAuthorizationRequestRepository requests) {
        this.requests = requests;
    }

    @Transactional
    public void save(
            UUID userId,
            ConnectedAccountProvider provider,
            String stateHash,
            String codeVerifierEncrypted,
            int encryptionKeyVersion,
            List<String> requestedScopes,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt) {
        requests.save(new OAuthAuthorizationRequest(
                userId,
                provider,
                stateHash,
                codeVerifierEncrypted,
                encryptionKeyVersion,
                requestedScopes,
                createdAt,
                expiresAt));
    }

    /**
     * Checks ownership and atomically consumes the request in one transaction.
     * The returned snapshot must be captured before the update because the
     * repository clears the persistence context.
     */
    @Transactional
    public Optional<ConsumedAuthorization> consume(
            String stateHash,
            UUID userId,
            OffsetDateTime now) {
        Optional<OAuthAuthorizationRequest> found = requests.findByStateHash(stateHash);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        OAuthAuthorizationRequest request = found.get();
        if (!request.getUserId().equals(userId)) {
            return Optional.empty();
        }

        ConsumedAuthorization snapshot = new ConsumedAuthorization(
                request.getUserId(),
                request.getCodeVerifierEncrypted(),
                request.getEncryptionKeyVersion(),
                request.getRequestedScopes(),
                request.getExpiresAt());

        int updated = requests.markConsumed(stateHash, now);
        return updated == 1 ? Optional.of(snapshot) : Optional.empty();
    }

    public record ConsumedAuthorization(
            UUID userId,
            String codeVerifierEncrypted,
            int encryptionKeyVersion,
            List<String> requestedScopes,
            OffsetDateTime expiresAt) {

        public ConsumedAuthorization {
            requestedScopes = requestedScopes == null ? List.of() : List.copyOf(requestedScopes);
        }
    }
}