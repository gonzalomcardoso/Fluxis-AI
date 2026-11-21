package com.fluxisai.subscription;
import java.math.BigDecimal;
public record CurrencySummaryResponse(String currency,BigDecimal monthly,BigDecimal annual){}
