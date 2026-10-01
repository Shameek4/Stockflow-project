package com.stockflow;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "sales_orders")
public class SalesOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 120)
  public String customer;

  @Column(nullable = false)
  public String createdBy;

  public Instant createdAt = Instant.now();

  @Enumerated(EnumType.STRING)
  public Status status = Status.CONFIRMED;

  @Column(precision = 14, scale = 2)
  public BigDecimal total = BigDecimal.ZERO;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
  @OrderColumn(name = "line_position")
  public List<Line> items = new ArrayList<>();

  public enum Status {
    CONFIRMED,
    CANCELLED
  }

  @Embeddable
  public static class Line {
    public Long productId;
    public String productName;
    public int quantity;

    @Column(precision = 12, scale = 2)
    public BigDecimal unitPrice;

    public Line() {}

    Line(Product p, int quantity) {
      productId = p.id;
      productName = p.name;
      this.quantity = quantity;
      unitPrice = p.price;
    }
  }

  protected SalesOrder() {}

  SalesOrder(String customer, String actor) {
    this.customer = customer;
    createdBy = actor;
  }
}
