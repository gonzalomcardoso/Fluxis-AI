package com.fluxisai.gmail;

import java.util.List;

/**
 * OAuth scopes requested from Google. Keep this list minimal and request
 * broader scopes only through an explicit incremental-authorization flow.
 */
public final class GmailOAuthScopes {

    public static final String OPENID = "openid";
    public static final String EMAIL = "email";
    public static final String GMAIL_METADATA = "https://www.googleapis.com/auth/gmail.metadata";

    /**
     * Broader scope required later for label changes and archiving. It must be
     * requested only after the user opts in to Gmail organization actions.
     */
    public static final String GMAIL_MODIFY = "https://www.googleapis.com/auth/gmail.modify";

    /**
     * Scope for draft creation in a later phase. The application never asks for
     * gmail.send or full-mailbox access.
     */
    public static final String GMAIL_COMPOSE = "https://www.googleapis.com/auth/gmail.compose";

    public static final List<String> BASELINE = List.of(OPENID, EMAIL, GMAIL_METADATA);

    private GmailOAuthScopes() {
    }
}
