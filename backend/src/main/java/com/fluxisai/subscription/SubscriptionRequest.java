package com.fluxisai.subscription;
import java.math.BigDecimal; import java.time.LocalDate; import jakarta.validation.constraints.*;
public record SubscriptionRequest(@NotBlank @Size(max=200) String name,@NotBlank @Size(max=200) String provider,@NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal amount,@NotBlank @Pattern(regexp="[A-Z]{3}") String currency,@NotNull SubscriptionPeriodicity periodicity,LocalDate nextChargeDate,LocalDate renewalDate,SubscriptionStatus status,@Size(max=120) String category,@Size(max=20000) String notes,@Size(max=2048) String managementUrl) {}
