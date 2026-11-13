package com.fluxisai.proposal;
import java.time.LocalDate; import java.util.*; import jakarta.validation.Valid; import jakarta.validation.constraints.*;
public record ProposalRequest(@NotNull UUID clientId,@NotBlank @Size(max=200) String title,@Size(max=20000) String description,@NotBlank @Pattern(regexp="[A-Z]{3}") String currency,LocalDate validUntil,ProposalStatus status,@NotEmpty List<@Valid ProposalItemRequest> items) {}
