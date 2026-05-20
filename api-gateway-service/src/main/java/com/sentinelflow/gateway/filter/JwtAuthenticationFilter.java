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

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator",
            "/api/v1/auth",
            "/api/v1/payments",
            "/api/v1/analytics",
            "/api/v1/fraud",
            "/api/v1/insights"
    );

    @Value("${jwt.secret:sentinelflow-dev-secret-key-minimum-256-bits-long}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            log.debug("Public path accessed: {} — skipping JWT validation", path);
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return unauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = validateToken(token);

            String tenantId = claims.get("tenantId", String.class);
            String userId   = claims.getSubject();
            String role     = claims.get("role", String.class);

            log.debug("JWT valid — userId: {}, tenantId: {}, role: {}", userId, tenantId, role);

            ServerWebExchange enrichedExchange = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.set("X-Tenant-Id", tenantId != null ? tenantId : "");
                        headers.set("X-User-Id",   userId   != null ? userId   : "");
                        headers.set("X-User-Role",  role     != null ? role     : "");
                    }))
                    .build();

            return chain.filter(enrichedExchange);

        } catch (ExpiredJwtException e) {
            return unauthorizedResponse(exchange, "Token has expired");
        } catch (SignatureException e) {
            return unauthorizedResponse(exchange, "Invalid token signature");
        } catch (MalformedJwtException e) {
            return unauthorizedResponse(exchange, "Malformed token");
        } catch (Exception e) {
            return unauthorizedResponse(exchange, "Token validation failed");
        }
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

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

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
