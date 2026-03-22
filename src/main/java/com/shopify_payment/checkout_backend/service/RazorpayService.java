package com.shopify_payment.checkout_backend.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class RazorpayService {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    /**
     * Creates a Razorpay order.
     *
     * @param amountPaise amount in paise (e.g. ₹100 → 10000)
     * @param receipt     unique receipt identifier (e.g. variant ID + timestamp)
     * @return map with orderId, amount, currency, keyId
     */
    public Map<String, Object> createOrder(long amountPaise, String receipt) throws Exception {
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject opts = new JSONObject();
        opts.put("amount", amountPaise);
        opts.put("currency", "INR");
        opts.put("receipt", receipt);

        Order order = client.orders.create(opts);

        return Map.of(
                "orderId", order.get("id"),
                "amount", amountPaise,
                "currency", "INR",
                "keyId", keyId
        );
    }

    /**
     * Verifies Razorpay HMAC signature to confirm payment authenticity.
     * Must be called before creating the Shopify order.
     */
    public boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}