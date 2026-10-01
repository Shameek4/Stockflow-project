package com.stockflow;

import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoData {
  @Bean
  @ConditionalOnProperty(name = "stockflow.demo.enabled", havingValue = "true")
  CommandLineRunner seed(ProductRepository products, InventoryService service) {
    return args -> {
      if (products.count() == 0) {
        service.addProduct(
            new Requests.ProductInput(
                "KB-001", "Mechanical Keyboard", "Accessories", new BigDecimal("2499.00"), 24, 5),
            "demo-seed");
        service.addProduct(
            new Requests.ProductInput(
                "MS-001", "Wireless Mouse", "Accessories", new BigDecimal("899.00"), 4, 5),
            "demo-seed");
        service.addProduct(
            new Requests.ProductInput(
                "MON-001", "24-inch Monitor", "Displays", new BigDecimal("10999.00"), 12, 3),
            "demo-seed");
      }
    };
  }
}
