package com.fluxisai.gmail;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Public connection metadata. It intentionally contains no OAuth credentials.
 */
public record ConnectedAccountResponse(
        UUID id,
        ConnectedAccountProvider provider,
        String providerAccountId,
        String accountEmail,
        List<String> grantedScopes,
        OffsetDateTime tokenExpiresAt,
        ConnectedAccountStatus status,
        OffsetDateTime connectedAt) {

    static ConnectedAccountResponse from(ConnectedAccount account) {
        return new ConnectedAccountResponse(
                account.getId(),
                account.getProvider(),
                account.getProviderAccountId(),
                account.getAccountEmail(),
                account.getGrantedScopes(),
                account.getTokenExpiresAt(),
                account.getStatus(),
                account.getConnectedAt());
    }
}
