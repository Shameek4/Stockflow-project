package com.stockflow;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = "sku"))
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, length = 40)
  public String sku;

  @Column(nullable = false, length = 120)
  public String name;

  @Column(nullable = false, length = 60)
  public String category;

  @Column(nullable = false, precision = 12, scale = 2)
  public BigDecimal price;

  public int stock;
  public int lowStockThreshold;
  public boolean active = true;

  protected Product() {}

  Product(String sku, String name, String category, BigDecimal price, int stock, int threshold) {
    this.sku = sku;
    this.name = name;
    this.category = category;
    this.price = price;
    this.stock = stock;
    this.lowStockThreshold = threshold;
  }
}
