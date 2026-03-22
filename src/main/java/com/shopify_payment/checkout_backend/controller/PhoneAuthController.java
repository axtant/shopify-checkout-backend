package com.shopify_payment.checkout_backend.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class PhoneAuthController {

    /**
     * POST /api/auth/verify-phone
     *
     * Receives a Firebase ID token from the frontend (after Firebase Phone Auth
     * verifies the OTP on the client side), validates it server-side, and returns
     * the verified phone number.
     *
     * Body:  { "idToken": "<firebase-id-token>" }
     * Response: { "phone": "9876543210" }  (10-digit, +91 stripped)
     */
    @PostMapping("/verify-phone")
    public ResponseEntity<Map<String, String>> verifyPhone(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");

        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "idToken is required"));
        }

        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            Object rawPhone = decoded.getClaims().get("phone_number");

            if (rawPhone == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "Token does not contain a verified phone number"));
            }

            String phone = rawPhone.toString();

            // Normalise to 10 digits (strip +91 or any country code)
            if (phone.startsWith("+91")) {
                phone = phone.substring(3);
            } else if (phone.startsWith("+")) {
                phone = phone.replaceAll("^\\+\\d{1,3}", "");
            }

            return ResponseEntity.ok(Map.of("phone", phone));

        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Invalid or expired token. Please try again."));
        }
    }
}
