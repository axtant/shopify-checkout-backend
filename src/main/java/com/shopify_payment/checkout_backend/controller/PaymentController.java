package com.shopify_payment.checkout_backend.controller;

import com.shopify_payment.checkout_backend.model.PaymentRequest;
import com.shopify_payment.checkout_backend.service.RazorpayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final RazorpayService razorpayService;

    public PaymentController(RazorpayService razorpayService) {
        this.razorpayService = razorpayService;
    }

    /**
     * POST /api/payment/create-order
     * Body: { amount (paise), currency, variantId, type }
     * Returns: { orderId, amount, currency, keyId }
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@Valid @RequestBody PaymentRequest req) throws Exception {
        String receipt = "rcpt_" + req.getVariantId() + "_" + System.currentTimeMillis();
        Map<String, Object> order = razorpayService.createOrder(req.getAmount(), receipt);
        return ResponseEntity.ok(order);
    }

    /**
     * POST /api/payment/verify
     * Body: { razorpayOrderId, razorpayPaymentId, razorpaySignature }
     * Returns: { verified: true } or 400 if signature mismatch
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> body) {
        String orderId = body.get("razorpayOrderId");
        String paymentId = body.get("razorpayPaymentId");
        String signature = body.get("razorpaySignature");

        boolean verified = razorpayService.verifySignature(orderId, paymentId, signature);
        if (!verified) {
            return ResponseEntity.badRequest().body(Map.of("verified", false, "error", "Signature mismatch"));
        }
        return ResponseEntity.ok(Map.of("verified", true));
    }
}