package com.inno.gateway.filter;

import static com.inno.gateway.exception.ExceptionMessages.INVALID_PATH;

import com.inno.gateway.exception.GatewayResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserCreationBlockerFilter implements GlobalFilter, Ordered {

  private final GatewayResponseWriter responseWriter;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getPath().value();

    if (request.getMethod().name().equalsIgnoreCase("POST")
        && path.matches("^/api/v1/users/\\d+$")) {
      return responseWriter.writeError(exchange, HttpStatus.NOT_FOUND, INVALID_PATH);
    }

    return chain.filter(exchange);
  }

  @Override
  public int getOrder() {
    return -5;
  }
}
