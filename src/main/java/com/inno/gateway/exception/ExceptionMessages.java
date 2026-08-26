package com.inno.gateway.exception;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ExceptionMessages {
  // JWT exceptions
  public static final String INVALID_JWT =
      "Full authentication is required to access this resource";

  // Method exceptions
  public static final String INVALID_PATH = "Invalid path or method";
}
