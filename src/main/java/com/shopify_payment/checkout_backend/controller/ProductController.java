package com.shopify_payment.checkout_backend.controller;

import com.shopify_payment.checkout_backend.service.ShopifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ShopifyService shopifyService;

    public ProductController(ShopifyService shopifyService) {
        this.shopifyService = shopifyService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * GET /api/product?variantId={id}
     * Returns product details + pricing (MRP, sale price, prepaid price, savings).
     */
    @GetMapping("/product")
    public ResponseEntity<Map<String, Object>> getProduct(
            @RequestParam long variantId,
            @RequestHeader("Origin") String origin) throws Exception {
        String domain = URI.create(origin).getHost();
        Map<String, Object> product = shopifyService.getProductByVariant(variantId, domain);
        return ResponseEntity.ok(product);
    }
}