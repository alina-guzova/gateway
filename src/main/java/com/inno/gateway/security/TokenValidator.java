package com.inno.gateway.security;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenValidator {

  public boolean isAccessToken(Claims claims) {
    String tokenType = claims.get("type", String.class);
    if (!"ACCESS".equals(tokenType)) {
      log.warn("Token validation failed: expected ACCESS token: {}", tokenType);
      return false;
    }
    return claims.getSubject() != null;
  }
}
