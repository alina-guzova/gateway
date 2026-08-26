package com.inno.gateway.filter;

import static com.inno.gateway.exception.ExceptionMessages.INVALID_JWT;
import static com.inno.gateway.util.ConfigConstants.USER_ID_HEADER;
import static com.inno.gateway.util.ConfigConstants.USER_ROLE_HEADER;

import com.inno.gateway.exception.GatewayResponseWriter;
import com.inno.gateway.security.JwtService;
import com.inno.gateway.security.TokenValidator;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtValidationFilter implements GlobalFilter, Ordered {

  private final JwtService jwtService;
  private final TokenValidator tokenValidator;
  private final GatewayResponseWriter responseWriter;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getPath().value();

    if (path.startsWith("/api/v1/auth")) {
      return chain.filter(exchange);
    }

    String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.warn("Access attempt with missing or invalid Authorization header on path: {}", path);
      return responseWriter.writeError(exchange, HttpStatus.UNAUTHORIZED, INVALID_JWT);
    }

    String jwt = authHeader.substring(7);

    try {
      Claims claims = jwtService.extractAllClaims(jwt);

      if (!tokenValidator.isAccessToken(claims)) {
        return responseWriter.writeError(exchange, HttpStatus.UNAUTHORIZED, INVALID_JWT);
      }

      String userId = claims.getSubject();
      String role = claims.get("role", String.class);

      ServerHttpRequest mutatedRequest =
          request
              .mutate()
              .headers(httpHeaders -> httpHeaders.remove(HttpHeaders.AUTHORIZATION))
              .header(USER_ID_HEADER, userId)
              .header(USER_ROLE_HEADER, role)
              .build();

      return chain.filter(exchange.mutate().request(mutatedRequest).build());
    } catch (Exception ex) {
      log.error("JWT validation failed: {}", ex.getMessage());
      return responseWriter.writeError(exchange, HttpStatus.UNAUTHORIZED, INVALID_JWT);
    }
  }

  @Override
  public int getOrder() {
    return -10;
  }
}
