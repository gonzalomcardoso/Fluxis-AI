package com.fluxisai.proposal;
import java.math.BigDecimal; import java.util.UUID; import jakarta.validation.constraints.*;
public record ProposalItemRequest(@NotNull UUID serviceId,@NotBlank @Size(max=500) String description,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal quantity,@NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal unitPrice) {}
