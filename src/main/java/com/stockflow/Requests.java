package com.stockflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class Requests {
  private Requests() {}

  public record ProductInput(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String sku,
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 60) String category,
      @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2)
          BigDecimal price,
      @Min(0) @Max(1000000) int stock,
      @Min(0) @Max(1000000) int lowStockThreshold) {}

  public record StockInput(
      @Min(-1000000) @Max(1000000) int delta, @NotBlank @Size(max = 200) String reason) {}

  public record OrderInput(
      @NotBlank @Size(max = 120) String customer,
      @NotEmpty @Size(max = 50) List<@NotNull @Valid ItemInput> items) {}

  public record ItemInput(@NotNull @Positive Long productId, @Min(1) @Max(10000) int quantity) {}
}
