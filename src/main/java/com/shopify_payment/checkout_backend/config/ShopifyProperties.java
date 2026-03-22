package com.shopify_payment.checkout_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "shopify")
public class ShopifyProperties {

    private String apiVersion;
    private List<ShopifyStoreConfig> stores = new ArrayList<>();

    public ShopifyStoreConfig getStore(String domain) {
        return stores.stream()
                .filter(s -> s.getDomain().equalsIgnoreCase(domain))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Shopify store: " + domain));
    }
}
