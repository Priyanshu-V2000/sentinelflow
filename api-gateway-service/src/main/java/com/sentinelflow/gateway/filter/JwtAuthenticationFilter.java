package com.sentinelflow.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT Authentication Filter
 *
 * This filter runs on EVERY request before it reaches any service.
 * It checks:
 *   1. Does the request have an Authorization header?
 *   2. Is the token format correct (Bearer <token>)?
 *   3. Is the token signature valid (not tampered)?
 *   4. Has the token expired?
 *
 * If all checks pass → request continues to the downstream service
 * If any check fails → 401 Unauthorized returned immediately
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // Paths that do NOT require a JWT token
    private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh"
    );

    @Value("${jwt.secret:sentinelflow-dev-secret-key-minimum-256-bits-long}")
    private String jwtSecret;

    /**
     * The main filter method — runs on every request
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Step 1: Skip JWT check for public paths
        if (isPublicPath(path)) {
            log.debug("Public path accessed: {} — skipping JWT validation", path);
            return chain.filter(exchange);
        }

        // Step 2: Get the Authorization header
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        // Step 3: Extract the token (remove "Bearer " prefix)
        String token = authHeader.substring(7);

        // Step 4: Validate the token
        try {
            Claims claims = validateToken(token);

            // Step 5: Add user info to headers so downstream services know who is calling
            String tenantId = claims.get("tenantId", String.class);
            String userId   = claims.getSubject();
            String role     = claims.get("role", String.class);

            log.debug("JWT valid — userId: {}, tenantId: {}, role: {}", userId, tenantId, role);

            // Pass enriched request downstream
            ServerWebExchange enrichedExchange = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.set("X-Tenant-Id", tenantId != null ? tenantId : "");
                        headers.set("X-User-Id",   userId   != null ? userId   : "");
                        headers.set("X-User-Role",  role     != null ? role     : "");
                    }))
                    .build();

            return chain.filter(enrichedExchange);

        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for path: {}", path);
            return unauthorizedResponse(exchange, "Token has expired");

        } catch (SignatureException e) {
            log.warn("JWT signature invalid for path: {}", path);
            return unauthorizedResponse(exchange, "Invalid token signature");

        } catch (MalformedJwtException e) {
            log.warn("JWT malformed for path: {}", path);
            return unauthorizedResponse(exchange, "Malformed token");

        } catch (Exception e) {
            log.error("JWT validation error for path: {}", path, e);
            return unauthorizedResponse(exchange, "Token validation failed");
        }
    }

    /**
     * Validates the JWT token and returns its claims (payload data)
     */
    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns a 401 Unauthorized response with a JSON error message
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"error":"UNAUTHORIZED","message":"%s","status":401}
                """.formatted(message);

        var buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * Check if a path is in the public (no-auth-required) list
     */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * Order = 1 means this filter runs FIRST before all other filters
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
