package com.fluxisai.gmail;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gmail/connection")
public class GmailConnectionController {

    private final ConnectedAccountService connectedAccounts;
    private final GmailConnectionService connectionService;

    public GmailConnectionController(
            ConnectedAccountService connectedAccounts,
            GmailConnectionService connectionService) {
        this.connectedAccounts = connectedAccounts;
        this.connectionService = connectionService;
    }

    @GetMapping("/status")
    @Operation(summary = "Get the current Gmail connection status")
    @SecurityRequirement(name = "sessionCookie")
    public ResponseEntity<ConnectedAccountResponse> status() {
        return connectedAccounts.currentUserConnection()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Disconnect Gmail and remove local credentials")
    @SecurityRequirement(name = "sessionCookie")
    public void disconnect() {
        connectionService.disconnectCurrentUser();
    }
}