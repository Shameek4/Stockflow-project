package com.stockflow;

import static com.stockflow.Requests.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final InventoryService service;
  private final ProductRepository products;
  private final OrderRepository orders;
  private final MovementRepository movements;

  public ApiController(
      InventoryService s, ProductRepository p, OrderRepository o, MovementRepository m) {
    service = s;
    products = p;
    orders = o;
    movements = m;
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of(
        "token",
        token.getToken(),
        "headerName",
        token.getHeaderName(),
        "parameterName",
        token.getParameterName());
  }

  @GetMapping("/me")
  public Map<String, Object> me(Authentication auth) {
    return Map.of(
        "username",
        auth.getName(),
        "roles",
        auth.getAuthorities().stream().map(Object::toString).toList());
  }

  @GetMapping("/products")
  public List<Product> products(@RequestParam(defaultValue = "") String q) {
    String term = q.toLowerCase(Locale.ROOT);
    return products.findAllByOrderByIdAsc().stream()
        .filter(
            p -> (p.name + " " + p.sku + " " + p.category).toLowerCase(Locale.ROOT).contains(term))
        .toList();
  }

  @PostMapping("/products")
  @ResponseStatus(HttpStatus.CREATED)
  public Product add(@Valid @RequestBody ProductInput input, Authentication a) {
    return service.addProduct(input, a.getName());
  }

  @PutMapping("/products/{id}")
  public Product update(
      @PathVariable Long id, @Valid @RequestBody ProductInput input, Authentication a) {
    return service.updateProduct(id, input, a.getName());
  }

  @PostMapping("/products/{id}/stock")
  public Product adjust(
      @PathVariable Long id, @Valid @RequestBody StockInput input, Authentication a) {
    return service.adjust(id, input, a.getName());
  }

  @PostMapping("/products/{id}/archive")
  public Product archive(@PathVariable Long id) {
    return service.archive(id);
  }

  @GetMapping("/orders")
  public List<SalesOrder> orders() {
    return orders.findAllByOrderByIdDesc();
  }

  @PostMapping("/orders")
  @ResponseStatus(HttpStatus.CREATED)
  public SalesOrder order(@Valid @RequestBody OrderInput input, Authentication a) {
    return service.placeOrder(input, a.getName());
  }

  @PostMapping("/orders/{id}/cancel")
  public SalesOrder cancel(@PathVariable Long id, Authentication a) {
    return service.cancel(id, a.getName());
  }

  @GetMapping("/movements")
  public List<StockMovement> movements() {
    return movements.findTop100ByOrderByIdDesc();
  }

  @GetMapping("/dashboard")
  @Transactional(readOnly = true)
  public Map<String, Object> dashboard() {
    List<Product> all = products.findAllByOrderByIdAsc();
    List<SalesOrder> sales = orders.findAllByOrderByIdDesc();
    List<SalesOrder> confirmed =
        sales.stream().filter(o -> o.status == SalesOrder.Status.CONFIRMED).toList();
    BigDecimal revenue =
        confirmed.stream().map(o -> o.total).reduce(BigDecimal.ZERO, BigDecimal::add);
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    BigDecimal daily =
        confirmed.stream()
            .filter(o -> o.createdAt.atZone(ZoneOffset.UTC).toLocalDate().equals(today))
            .map(o -> o.total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal value =
        all.stream()
            .filter(p -> p.active)
            .map(p -> p.price.multiply(BigDecimal.valueOf(p.stock)))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Map<String, Long> units = new TreeMap<>();
    confirmed.forEach(
        o -> o.items.forEach(l -> units.merge(l.productName, (long) l.quantity, Long::sum)));
    return Map.of(
        "activeProducts",
        all.stream().filter(p -> p.active).count(),
        "confirmedOrders",
        confirmed.size(),
        "revenue",
        revenue,
        "todayRevenue",
        daily,
        "inventoryValue",
        value,
        "lowStock",
        all.stream().filter(p -> p.active && p.stock <= p.lowStockThreshold).toList(),
        "topProducts",
        units.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .map(e -> Map.of("name", e.getKey(), "units", e.getValue()))
            .toList());
  }
}
