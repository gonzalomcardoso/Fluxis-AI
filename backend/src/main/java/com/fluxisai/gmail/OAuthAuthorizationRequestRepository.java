package com.fluxisai.gmail;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuthAuthorizationRequestRepository
        extends JpaRepository<OAuthAuthorizationRequest, UUID> {

    Optional<OAuthAuthorizationRequest> findByStateHash(String stateHash);

    /**
     * Atomically consumes a pending authorization request.
     *
     * <p>Requires an active transaction. The returned update count is the only
     * source of truth: 1 means this call consumed the request; 0 means it was
     * missing, expired, or already consumed.</p>
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE OAuthAuthorizationRequest request
               SET request.consumedAt = :now
             WHERE request.stateHash = :stateHash
               AND request.consumedAt IS NULL
               AND request.expiresAt > :now
            """)
    int markConsumed(
            @Param("stateHash") String stateHash,
            @Param("now") OffsetDateTime now);
}