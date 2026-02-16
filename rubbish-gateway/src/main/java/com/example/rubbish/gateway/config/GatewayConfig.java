package com.example.rubbish.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return new KeyResolver() {
            @Override
            public Mono<String> resolve(ServerWebExchange exchange) {
                String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                return Mono.just(ip);
            }
        };
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return new KeyResolver() {
            @Override
            public Mono<String> resolve(ServerWebExchange exchange) {
                String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
                return Mono.just(userId != null ? userId : "anonymous");
            }
        };
    }

    @Bean
    public KeyResolver apiKeyResolver() {
        return new KeyResolver() {
            @Override
            public Mono<String> resolve(ServerWebExchange exchange) {
                String path = exchange.getRequest().getPath().value();
                return Mono.just(path);
            }
        };
    }
}
