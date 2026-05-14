package com.sentinelflow.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final long   WINDOW_MS   = 60_000L;  // 1 minute
    private static final long   MAX_REQUESTS = 100L;     // 100 per minute

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final RedisScript<List<Long>> rateLimitScript;

    public RateLimitFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = RedisScript.of(
                new ClassPathResource("scripts/sliding_window.lua"),
                (Class<List<Long>>) (Class<?>) List.class
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Skip rate limiting for actuator endpoints
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        // Build Redis key: rl:{tenantId}:{path}
        String tenantId = exchange.getRequest().getHeaders()
                .getFirst("X-Tenant-Id");
        String key = "rl:" + (tenantId != null ? tenantId : "anonymous") + ":" + path;
        long now = System.currentTimeMillis();

        return redisTemplate.execute(
                rateLimitScript,
                List.of(key),
                List.of(
                        String.valueOf(WINDOW_MS),
                        String.valueOf(MAX_REQUESTS),
                        String.valueOf(now)
                )
        ).next().flatMap(result -> {
            long allowed   = result.get(0);
            long remaining = result.get(1);
            long resetAt   = result.get(2);

            // Add rate limit headers to every response
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit",     String.valueOf(MAX_REQUESTS));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));
            exchange.getResponse().getHeaders().add("X-RateLimit-Reset",     String.valueOf(resetAt));

            if (allowed == 1) {
                return chain.filter(exchange);
            } else {
                log.warn("Rate limit exceeded for key: {}", key);
                return tooManyRequests(exchange);
            }
        }).onErrorResume(e -> {
            // Redis unavailable → fail open (allow request)
            log.error("Redis error in rate limiter — failing open: {}", e.getMessage());
            return chain.filter(exchange);
        });
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"error":"RATE_LIMIT_EXCEEDED","message":"Too many requests. Please slow down.","status":429}
                """;
        var buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 2; // Runs after JWT filter (order -1)
    }
}
