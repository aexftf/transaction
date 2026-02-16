package com.example.rubbish.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.example.rubbish.gateway.config.WhiteListConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";

    private final WhiteListConfig whiteListConfig;
    private final String jwtSecret = "rubbish-secret-key-2024";

    public AuthGlobalFilter(WhiteListConfig whiteListConfig) {
        this.whiteListConfig = whiteListConfig;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.info("请求路径：{}", path);

        // 检查白名单
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        // 获取Token
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange.getResponse(), "未携带Token");
        }

        // 验证Token
        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                return unauthorized(exchange.getResponse(), "Token无效");
            }

            // 检查Token是否过期
            if (isTokenExpired(claims)) {
                return unauthorized(exchange.getResponse(), "Token已过期");
            }

            // 将用户信息添加到请求头
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header(USER_ID_HEADER, String.valueOf(userId))
                    .header(USERNAME_HEADER, username)
                    .build();

            exchange = exchange.mutate().request(modifiedRequest).build();

        } catch (Exception e) {
            log.error("Token验证失败：{}", e.getMessage());
            return unauthorized(exchange.getResponse(), "Token验证失败");
        }

        return chain.filter(exchange);
    }

    private boolean isWhiteList(String path) {
        List<String> whiteList = whiteListConfig.getWhiteList();
        return whiteList.stream().anyMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String authorization = headers.getFirst(AUTH_HEADER);

        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("解析Token失败：{}", e.getMessage());
            return null;
        }
    }

    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", message);
        result.put("data", null);
        result.put("timestamp", System.currentTimeMillis());

        DataBuffer buffer = response.bufferFactory()
                .wrap(JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
