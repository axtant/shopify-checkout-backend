package com.shopify_payment.checkout_backend.config;

import lombok.Data;

@Data
public class ShopifyStoreConfig {
    private String domain;
    private String clientId;
    private String clientSecret;
}