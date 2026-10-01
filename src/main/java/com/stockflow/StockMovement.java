package com.stockflow;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long productId;
  public int delta;
  public int resultingStock;
  public String reason;
  public String actor;
  public Instant createdAt = Instant.now();

  protected StockMovement() {}

  StockMovement(Product p, int delta, String reason, String actor) {
    productId = p.id;
    this.delta = delta;
    resultingStock = p.stock;
    this.reason = reason;
    this.actor = actor;
  }
}
