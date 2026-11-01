package com.mini_wallet.gateway_service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTests {
    private static final AtomicReference<ObservedRequest> LAST_REQUEST = new AtomicReference<>();
    private static final AtomicInteger REQUEST_COUNT = new AtomicInteger();
    private static final HttpServer BACKEND = startBackend();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @Autowired
    private Environment environment;

    @DynamicPropertySource
    static void backendUrls(DynamicPropertyRegistry registry) {
        for (String service : new String[]{"USER", "LEDGER", "PAYMENT", "STATEMENT"}) {
            registry.add(service + "_SERVICE_URL", () -> "http://localhost:" + BACKEND.getAddress().getPort());
        }
    }

    @AfterAll
    static void stopBackend() {
        BACKEND.stop(0);
    }

    @Test
    void forwardsPaymentMethodHeadersBodyAndResponseStatus() throws Exception {
        String body = "{\"type\":\"DEPOSIT\",\"amount\":\"100.00\"}";
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri("/api/v1/payments"))
                .header("Authorization", "Bearer test-token")
                .header("Idempotency-Key", "same-payment-key")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).isEqualTo("{\"proxied\":true}");
        assertThat(LAST_REQUEST.get()).isEqualTo(new ObservedRequest(
                "POST", "/api/v1/payments", "Bearer test-token", "same-payment-key", body));
    }

    @Test
    void forwardsPublicRoutesAndQueryParameters() throws Exception {
        for (String path : new String[]{"/api/v1/auth/login", "/api/v1/users/me",
                "/api/v1/wallets/123", "/api/v1/payments/123",
                "/api/v1/statement?walletId=123&from=2026-01-01T00:00:00Z&limit=1"}) {
            HttpResponse<String> response = get(path);
            assertThat(response.statusCode()).as(path).isEqualTo(200);
            URI expectedUri = URI.create(path);
            URI forwardedUri = URI.create(LAST_REQUEST.get().uri());
            assertThat(forwardedUri.getPath()).isEqualTo(expectedUri.getPath());
            if (expectedUri.getQuery() != null) {
                assertThat(forwardedUri.getQuery().split("&"))
                        .containsExactlyInAnyOrder(expectedUri.getQuery().split("&"));
            }
        }
    }

    @Test
    void rewritesAllFourDocumentationRoutes() throws Exception {
        for (String service : new String[]{"user", "ledger", "payment", "statement"}) {
            assertThat(get("/openapi/" + service).statusCode()).isEqualTo(200);
            assertThat(LAST_REQUEST.get().uri()).isEqualTo("/v3/api-docs");
        }
    }

    @Test
    void doesNotForwardInternalOperationsOrUnknownDocumentationRoutes() throws Exception {
        int before = REQUEST_COUNT.get();
        assertThat(get("/internal/v1/operations").statusCode()).isEqualTo(404);
        assertThat(get("/openapi/unknown").statusCode()).isEqualTo(404);
        assertThat(REQUEST_COUNT.get()).isEqualTo(before);
    }

    @Test
    void forwardsProblemDetailsWithoutDroppingTheBusinessCode() throws Exception {
        HttpResponse<String> response = get("/api/v1/wallets/missing");
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/problem+json");
        assertThat(response.body()).isEqualTo("{\"status\":404,\"code\":\"WALLET_NOT_FOUND\"}");
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + environment.getProperty("local.server.port") + path);
    }

    private static HttpServer startBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                REQUEST_COUNT.incrementAndGet();
                LAST_REQUEST.set(new ObservedRequest(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        exchange.getRequestHeaders().getFirst("Idempotency-Key"),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                boolean missing = exchange.getRequestURI().getPath().endsWith("/missing");
                byte[] response = (missing ? "{\"status\":404,\"code\":\"WALLET_NOT_FOUND\"}" : "{\"proxied\":true}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", missing ? "application/problem+json" : "application/json");
                exchange.sendResponseHeaders(missing ? 404 : exchange.getRequestMethod().equals("POST") ? 201 : 200, response.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(response);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private record ObservedRequest(String method, String uri, String authorization, String idempotencyKey, String body) {}
}
