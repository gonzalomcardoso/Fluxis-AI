package com.fluxisai.proposal;
import java.math.BigDecimal; import java.util.UUID; import jakarta.validation.constraints.*;
public record ProposalItemRequest(@NotNull UUID serviceId,@NotBlank @Size(max=500) String description,@NotNull @DecimalMin("0.0001") @Digits(integer=15,fraction=4) BigDecimal quantity,@NotNull @DecimalMin("0") @Digits(integer=15,fraction=4) BigDecimal unitPrice) {}
