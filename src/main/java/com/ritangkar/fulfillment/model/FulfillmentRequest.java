package com.ritangkar.fulfillment.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record FulfillmentRequest(
        @NotBlank String customerCity,
        @NotEmpty @Valid List<OrderItem> items,
        @Positive int requestedSlaDays
) {
}
