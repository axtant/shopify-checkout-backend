package com.shopify_payment.checkout_backend.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class OrderRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Pattern(regexp = "\\d{10}", message = "Phone must be 10 digits")
    private String phone;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String address1;

    @NotBlank
    private String city;

    @NotBlank
    private String state;

    @Pattern(regexp = "\\d{6}", message = "Pincode must be 6 digits")
    private String pincode;

    @Positive
    private long variantId;

    @Positive
    private int quantity;

    // "prepaid" or "cod"
    @NotBlank
    private String paymentType;

    // Only required for prepaid orders
    private String paymentId;

    // Amount in rupees (used to record transaction for prepaid)
    private double amount;

    // "standard" or "express"
    private String deliveryType;
}