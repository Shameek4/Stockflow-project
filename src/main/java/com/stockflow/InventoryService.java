package com.stockflow;

import static com.stockflow.Requests.*;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {
  private final ProductRepository products;
  private final OrderRepository orders;
  private final MovementRepository movements;

  public InventoryService(
      ProductRepository products, OrderRepository orders, MovementRepository movements) {
    this.products = products;
    this.orders = orders;
    this.movements = movements;
  }

  public Product addProduct(ProductInput input, String actor) {
    String sku = input.sku().toUpperCase(Locale.ROOT);
    if (products.existsBySku(sku)) throw new Conflict("SKU already exists");
    Product p =
        products.save(
            new Product(
                sku,
                input.name().trim(),
                input.category().trim(),
                input.price(),
                input.stock(),
                input.lowStockThreshold()));
    movements.save(new StockMovement(p, input.stock(), "Opening stock", actor));
    return p;
  }

  public Product updateProduct(Long id, ProductInput input, String actor) {
    Product p = lockProduct(id);
    String sku = input.sku().toUpperCase(Locale.ROOT);
    if (!p.sku.equals(sku) && products.existsBySku(sku)) throw new Conflict("SKU already exists");
    // Quantity changes must go through adjustment, so every change has a reason and an audit
    // record.
    if (input.stock() != p.stock) throw new Conflict("Use stock adjustment to change quantity");
    p.sku = sku;
    p.name = input.name().trim();
    p.category = input.category().trim();
    p.price = input.price();
    p.lowStockThreshold = input.lowStockThreshold();
    return p;
  }

  public Product adjust(Long id, StockInput input, String actor) {
    if (input.delta() == 0) throw new Conflict("Adjustment must be nonzero");
    Product p = lockProduct(id);
    if (!p.active) throw new Conflict("Product is archived");
    long next = (long) p.stock + input.delta();
    if (next < 0 || next > 1000000)
      throw new Conflict("Resulting stock must be between 0 and 1,000,000");
    p.stock = (int) next;
    movements.save(new StockMovement(p, input.delta(), input.reason().trim(), actor));
    return p;
  }

  public Product archive(Long id) {
    Product p = lockProduct(id);
    p.active = false;
    return p;
  }

  public SalesOrder placeOrder(OrderInput input, String actor) {
    // Lock in ascending ID order to reduce deadlock risk across multi-product orders.
    SortedMap<Long, Integer> requested = new TreeMap<>();
    for (ItemInput item : input.items()) {
      if (requested.putIfAbsent(item.productId(), item.quantity()) != null)
        throw new Conflict("Duplicate product in order");
    }
    SalesOrder order = new SalesOrder(input.customer().trim(), actor);
    for (var item : requested.entrySet()) {
      Product p = lockProduct(item.getKey());
      int qty = item.getValue();
      if (!p.active) throw new Conflict("Product is archived: " + p.name);
      if (p.stock < qty) throw new Conflict("Insufficient stock: " + p.name);
      p.stock -= qty;
      order.items.add(new SalesOrder.Line(p, qty));
      order.total = order.total.add(p.price.multiply(BigDecimal.valueOf(qty)));
      if (order.total.compareTo(new BigDecimal("999999999999.99")) > 0)
        throw new Conflict("Order total exceeds supported limit");
    }
    orders.save(order);
    for (SalesOrder.Line line : order.items) {
      Product p = products.findById(line.productId).orElseThrow();
      movements.save(new StockMovement(p, -line.quantity, "Order #" + order.id, actor));
    }
    return order;
  }

  public SalesOrder cancel(Long id, String actor) {
    SalesOrder order = orders.findLockedById(id).orElseThrow(() -> new Missing("Order not found"));
    if (order.status == SalesOrder.Status.CANCELLED) return order;
    List<SalesOrder.Line> lines = new ArrayList<>(order.items);
    lines.sort(Comparator.comparing(l -> l.productId));
    for (SalesOrder.Line line : lines) {
      Product p = lockProduct(line.productId);
      long next = (long) p.stock + line.quantity;
      if (next > 1000000) throw new Conflict("Cancellation exceeds stock limit for " + p.name);
      p.stock = (int) next;
      movements.save(new StockMovement(p, line.quantity, "Cancel order #" + id, actor));
    }
    order.status = SalesOrder.Status.CANCELLED;
    return order;
  }

  private Product lockProduct(Long id) {
    return products.findLockedById(id).orElseThrow(() -> new Missing("Product not found"));
  }

  public static class Conflict extends RuntimeException {
    public Conflict(String message) {
      super(message);
    }
  }

  public static class Missing extends RuntimeException {
    public Missing(String message) {
      super(message);
    }
  }
}
