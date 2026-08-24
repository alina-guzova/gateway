package com.inno.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayResponseWriter {

  private final ObjectMapper objectMapper;

  public Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    ExceptionResponseDTO errorDTO = ExceptionResponseDTO.of(status, message);

    try {
      byte[] bytes = objectMapper.writeValueAsBytes(errorDTO);
      DataBuffer buffer = response.bufferFactory().wrap(bytes);
      return response.writeWith(Mono.just(buffer));
    } catch (Exception e) {
      log.error("Failed to serialize error response: {}", e.getMessage(), e);
      return response.setComplete();
    }
  }
}
