package com.shopify_payment.checkout_backend.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PaymentRequest {

    @Positive
    private long amount;          // amount in paise

    @NotBlank
    private String currency;      // "INR"

    @Positive
    private long variantId;

    // "prepaid" or "cod"
    @NotBlank
    private String type;
}