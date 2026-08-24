package com.inno.gateway.configuration;

import static com.inno.gateway.util.ConfigConstants.TRACE_ID_HEADER;
import static com.inno.gateway.util.ConfigConstants.TRACE_ID_KEY;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class GatewayLoggingFilter implements WebFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    long startTime = System.currentTimeMillis();

    String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
    if (traceId == null || traceId.isEmpty()) {
      traceId = UUID.randomUUID().toString().substring(0, 8);
    }

    final String finalTraceId = traceId;
    MDC.put(TRACE_ID_KEY, finalTraceId);

    ServerWebExchange mutatedExchange =
        exchange.mutate().request(builder -> builder.header(TRACE_ID_HEADER, finalTraceId)).build();

    String path = mutatedExchange.getRequest().getURI().getPath();
    String method = mutatedExchange.getRequest().getMethod().name();

    log.info("Incoming Request {} {}", method, path);

    return chain
        .filter(mutatedExchange)
        .doOnEach(
            signal -> {
              if (signal.isOnComplete() || signal.isOnError()) {
                long duration = System.currentTimeMillis() - startTime;

                Object targetUrlAttr =
                    exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                String targetUrl = targetUrlAttr != null ? targetUrlAttr.toString() : "UNKNOWN";

                Object routeAttr = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                String routeId = "UNKNOWN";
                if (routeAttr instanceof Route route) {
                  routeId = route.getId();
                }
                int statusCode =
                    mutatedExchange.getResponse().getStatusCode() != null
                        ? mutatedExchange.getResponse().getStatusCode().value()
                        : 500;

                log.info(
                    "Outgoing response {} {} | Status: {} | Target: {} | Route: {} | Duration: {}ms",
                    method,
                    path,
                    statusCode,
                    targetUrl,
                    routeId,
                    duration);
                MDC.clear();
              }
            });
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
