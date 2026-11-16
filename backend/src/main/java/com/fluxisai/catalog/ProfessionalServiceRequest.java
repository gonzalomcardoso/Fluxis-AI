package com.fluxisai.catalog;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record ProfessionalServiceRequest(
        @NotBlank @Size(max=200) String name,
        @Size(max=20000) String description,
        @Size(max=120) String category,
        @NotNull PricingModel pricingModel,
        @NotNull @DecimalMin(value="0.01") @Digits(integer=15,fraction=4) BigDecimal rate,
        @NotBlank @Pattern(regexp="[A-Z]{3}",message="La moneda debe tener tres letras mayúsculas") String currency,
        Boolean available,
        ProfessionalServiceStatus status,
        @Size(max=20000) String notes) {}
