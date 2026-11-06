package com.fluxisai.auth;

public record CsrfTokenResponse(String token, String headerName) {
}
