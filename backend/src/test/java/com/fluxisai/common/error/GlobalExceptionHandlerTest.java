package com.fluxisai.common.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {
    @Test
    void mapsForeignKeysRestrictionsToConflict() {
        var response = new GlobalExceptionHandler()
                .handleDataIntegrityViolation(new DataIntegrityViolationException("restricted"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflicto", response.getBody().getTitle());
    }
}

