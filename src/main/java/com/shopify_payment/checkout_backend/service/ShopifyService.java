package com.shopify_payment.checkout_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shopify_payment.checkout_backend.config.ShopifyProperties;
import com.shopify_payment.checkout_backend.config.ShopifyStoreConfig;
import com.shopify_payment.checkout_backend.model.OrderRequest;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ShopifyService {

    private final ShopifyProperties shopifyProperties;

    @Value("${razorpay.prepaid-discount-percent}")
    private int prepaidDiscountPercent;

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Per-store token cache: domain → access token
    private final ConcurrentHashMap<String, String> tokenCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> tokenExpiry = new ConcurrentHashMap<>();

    public ShopifyService(ShopifyProperties shopifyProperties) {
        this.shopifyProperties = shopifyProperties;
    }

    private String getAccessToken(String domain) throws Exception {
        Long expiry = tokenExpiry.get(domain);
        if (expiry != null && System.currentTimeMillis() < expiry - 60_000) {
            return tokenCache.get(domain);
        }

        ShopifyStoreConfig store = shopifyProperties.getStore(domain);

        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", store.getClientId())
                .add("client_secret", store.getClientSecret())
                .build();

        Request request = new Request.Builder()
                .url("https://" + domain + "/admin/oauth/access_token")
                .post(formBody)
                .build();

        try (Response resp = http.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new RuntimeException("Shopify token fetch failed for " + domain + ": " + resp.code() + " " + resp.body().string());
            }
            JsonNode json = mapper.readTree(resp.body().string());
            String token = json.at("/access_token").asText();
            long expiresIn = json.at("/expires_in").asLong(86400);

            tokenCache.put(domain, token);
            tokenExpiry.put(domain, System.currentTimeMillis() + expiresIn * 1000);
            return token;
        }
    }

    public Map<String, Object> getProductByVariant(long variantId, String domain) throws Exception {
        String baseUrl = "https://" + domain;
        String apiVersion = shopifyProperties.getApiVersion();
        String token = getAccessToken(domain);

        String url = baseUrl + "/admin/api/" + apiVersion + "/variants/" + variantId + ".json";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Shopify-Access-Token", token)
                .get()
                .build();

        try (Response resp = http.newCall(request).execute()) {
            if (!resp.isSuccessful()) {
                throw new RuntimeException("Shopify variant fetch failed: " + resp.code());
            }
            JsonNode variant = mapper.readTree(resp.body().string()).at("/variant");

            long productId = variant.at("/product_id").asLong();
            JsonNode product = fetchProduct(productId, domain, token, apiVersion);

            double mrp = variant.at("/compare_at_price").asDouble();
            double price = variant.at("/price").asDouble();
            double discount = mrp - price;
            double prepaidSaving = Math.round(price * prepaidDiscountPercent) / 100.0;
            double prepaidPrice = price - prepaidSaving;
            double totalSaving = mrp - prepaidPrice;

            return Map.of(
                    "name", product.at("/title").asText(),
                    "image", product.at("/image/src").asText(""),
                    "mrp", mrp,
                    "price", price,
                    "discount", discount,
                    "prepaidPrice", prepaidPrice,
                    "prepaidDiscount", prepaidSaving,
                    "prepaidSaving", totalSaving
            );
        }
    }

    private JsonNode fetchProduct(long productId, String domain, String token, String apiVersion) throws Exception {
        String url = "https://" + domain + "/admin/api/" + apiVersion + "/products/" + productId + ".json";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Shopify-Access-Token", token)
                .get()
                .build();

        try (Response resp = http.newCall(request).execute()) {
            return mapper.readTree(resp.body().string()).at("/product");
        }
    }

    public Map<String, Object> createOrder(OrderRequest req, String domain) throws Exception {
        String token = getAccessToken(domain);
        String apiVersion = shopifyProperties.getApiVersion();

        ObjectNode lineItem = mapper.createObjectNode();
        lineItem.put("variant_id", req.getVariantId());
        lineItem.put("quantity", req.getQuantity());

        ObjectNode addr = mapper.createObjectNode();
        addr.put("first_name", req.getFirstName());
        addr.put("last_name", req.getLastName());
        addr.put("address1", req.getAddress1());
        addr.put("city", req.getCity());
        addr.put("province", req.getState());
        addr.put("zip", req.getPincode());
        addr.put("country", "IN");
        addr.put("phone", req.getPhone());

        ObjectNode order = mapper.createObjectNode();
        order.set("line_items", mapper.createArrayNode().add(lineItem));
        order.set("shipping_address", addr);
        order.set("billing_address", addr);
        order.put("email", req.getEmail());
        order.put("send_receipt", true);

        if ("prepaid".equals(req.getPaymentType())) {
            ObjectNode txn = mapper.createObjectNode();
            txn.put("kind", "sale");
            txn.put("status", "success");
            txn.put("amount", req.getAmount());
            txn.put("gateway", "razorpay");
            txn.put("authorization", req.getPaymentId());
            order.set("transactions", mapper.createArrayNode().add(txn));
            order.put("financial_status", "paid");
        } else {
            order.put("financial_status", "pending");
        }

        ObjectNode body = mapper.createObjectNode();
        body.set("order", order);

        Request request = new Request.Builder()
                .url("https://" + domain + "/admin/api/" + apiVersion + "/orders.json")
                .addHeader("X-Shopify-Access-Token", token)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(mapper.writeValueAsString(body), JSON))
                .build();

        try (Response resp = http.newCall(request).execute()) {
            String responseBody = resp.body().string();
            if (!resp.isSuccessful()) {
                throw new RuntimeException("Shopify order creation failed: " + resp.code() + " " + responseBody);
            }
            JsonNode result = mapper.readTree(responseBody);
            return Map.of(
                    "orderNumber", result.at("/order/order_number").asText(),
                    "orderId", result.at("/order/id").asText()
            );
        }
    }
}
