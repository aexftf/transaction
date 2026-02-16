package com.example.rubbish.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private static final String START_TIME_ATTR = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        String requestId = request.getId();
        String method = request.getMethod().name();
        String path = request.getPath().value();
        String ip = request.getRemoteAddress().getAddress().getHostAddress();

        log.info("请求开始 - ID: {}, IP: {}, Method: {}, Path: {}", requestId, ip, method, path);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME_ATTR);
            if (startTime != null) {
                long executeTime = System.currentTimeMillis() - startTime;
                int statusCode = exchange.getResponse().getStatusCode() != null ?
                        exchange.getResponse().getStatusCode().value() : 0;
                log.info("请求结束 - ID: {}, Status: {}, Time: {}ms", requestId, statusCode, executeTime);
            }
        }));
    }

    @Override
    public int getOrder() {
        return -99;
    }
}
