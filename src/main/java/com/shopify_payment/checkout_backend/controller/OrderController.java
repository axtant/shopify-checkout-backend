package com.shopify_payment.checkout_backend.controller;

import com.shopify_payment.checkout_backend.model.OrderRequest;
import com.shopify_payment.checkout_backend.service.ShopifyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final ShopifyService shopifyService;

    public OrderController(ShopifyService shopifyService) {
        this.shopifyService = shopifyService;
    }

    /**
     * POST /api/order/create
     * Creates a Shopify order for both prepaid and COD payments.
     * For prepaid: requires paymentId (Razorpay payment ID) — verify signature first!
     * Returns: { orderNumber, orderId }
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createOrder(
            @Valid @RequestBody OrderRequest req,
            @RequestHeader("Origin") String origin) throws Exception {
        // Guard: prepaid orders must have a paymentId
        if ("prepaid".equals(req.getPaymentType()) &&
                (req.getPaymentId() == null || req.getPaymentId().isBlank())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "paymentId is required for prepaid orders"));
        }

        String domain = URI.create(origin).getHost();
        Map<String, Object> result = shopifyService.createOrder(req, domain);
        return ResponseEntity.ok(result);
    }
}
