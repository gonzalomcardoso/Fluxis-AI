package com.fluxisai.subscription;
import java.util.List;
public record SubscriptionSummaryResponse(List<CurrencySummaryResponse> currencies,List<SubscriptionResponse> upcomingRenewals){}
