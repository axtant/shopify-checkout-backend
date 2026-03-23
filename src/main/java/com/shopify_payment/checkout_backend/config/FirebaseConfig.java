package com.shopify_payment.checkout_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    // Set in application.yml: firebase.service-account-path
    // Leave blank to use GOOGLE_APPLICATION_CREDENTIALS env var instead
    @Value("${firebase.service-account-path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) return;

        GoogleCredentials credentials;

        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(serviceAccountPath);
            if (stream == null) throw new IOException("Firebase service account not found on classpath: " + serviceAccountPath);
            credentials = GoogleCredentials.fromStream(stream);
            log.info("[Firebase] Initialized with service account: {}", serviceAccountPath);
        } else {
            // Falls back to GOOGLE_APPLICATION_CREDENTIALS environment variable
            credentials = GoogleCredentials.getApplicationDefault();
            log.info("[Firebase] Initialized with application default credentials");
        }

        FirebaseApp.initializeApp(
                FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build()
        );
    }
}
