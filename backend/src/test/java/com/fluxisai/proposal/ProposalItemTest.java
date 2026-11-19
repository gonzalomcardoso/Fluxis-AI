package com.fluxisai.proposal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ProposalItemTest {
    @Test
    void calculatesAmountWithBigDecimalAndRoundsToDatabaseScale() {
        var item = new ProposalItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Consulting",
                new BigDecimal("1.23"),
                new BigDecimal("2.35"));

        assertEquals(new BigDecimal("2.35"), item.getUnitPrice());
        assertEquals(new BigDecimal("2.89"), item.getAmount());
        assertEquals(2, item.getAmount().scale());
    }
}