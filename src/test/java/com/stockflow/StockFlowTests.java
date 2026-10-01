package com.stockflow;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:stockflow-tests;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "stockflow.demo.enabled=false",
      "stockflow.admin.password=TestAdminPassword123",
      "stockflow.staff.password=TestStaffPassword123"
    })
@AutoConfigureMockMvc
class StockFlowTests {
  @Autowired InventoryService service;
  @Autowired ProductRepository products;
  @Autowired OrderRepository orders;
  @Autowired MovementRepository movements;
  @Autowired MockMvc mvc;

  @BeforeEach
  void clean() {
    movements.deleteAll();
    orders.deleteAll();
    products.deleteAll();
  }

  Product product(String sku, int stock) {
    return service.addProduct(
        new Requests.ProductInput(
            sku, "Test product", "Accessories", new BigDecimal("19.95"), stock, 2),
        "admin");
  }

  Requests.OrderInput order(Long id, int qty) {
    return new Requests.OrderInput("Test customer", List.of(new Requests.ItemInput(id, qty)));
  }

  @Test
  void orderDeductsStockAndCalculatesMoneyExactly() {
    Product p = product("A", 10);
    SalesOrder o = service.placeOrder(order(p.id, 3), "staff");
    assertThat(o.total).isEqualByComparingTo("59.85");
    assertThat(products.findById(p.id).orElseThrow().stock).isEqualTo(7);
    assertThat(movements.count()).isEqualTo(2);
  }

  @Test
  void insufficientStockRollsBackWholeOrder() {
    Product a = product("A", 5), b = product("B", 1);
    assertThatThrownBy(
            () ->
                service.placeOrder(
                    new Requests.OrderInput(
                        "Customer",
                        List.of(new Requests.ItemInput(a.id, 2), new Requests.ItemInput(b.id, 2))),
                    "staff"))
        .isInstanceOf(InventoryService.Conflict.class);
    assertThat(products.findById(a.id).orElseThrow().stock).isEqualTo(5);
    assertThat(orders.count()).isZero();
    assertThat(movements.count()).isEqualTo(2);
  }

  @Test
  void cancellationRestoresStockOnlyOnce() {
    Product p = product("A", 10);
    SalesOrder o = service.placeOrder(order(p.id, 3), "staff");
    service.cancel(o.id, "staff");
    service.cancel(o.id, "staff");
    assertThat(products.findById(p.id).orElseThrow().stock).isEqualTo(10);
    assertThat(orders.findById(o.id).orElseThrow().status).isEqualTo(SalesOrder.Status.CANCELLED);
    assertThat(movements.count()).isEqualTo(3);
  }

  @Test
  void archivedProductsCannotBeOrderedButCancellationRestoresThem() {
    Product p = product("A", 5);
    SalesOrder o = service.placeOrder(order(p.id, 1), "staff");
    service.archive(p.id);
    assertThatThrownBy(() -> service.placeOrder(order(p.id, 1), "staff"))
        .isInstanceOf(InventoryService.Conflict.class);
    service.cancel(o.id, "staff");
    assertThat(products.findById(p.id).orElseThrow().stock).isEqualTo(5);
  }

  @Test
  void negativeAdjustmentCannotMakeStockNegative() {
    Product p = product("A", 2);
    assertThatThrownBy(() -> service.adjust(p.id, new Requests.StockInput(-3, "Damage"), "admin"))
        .isInstanceOf(InventoryService.Conflict.class);
    assertThat(products.findById(p.id).orElseThrow().stock).isEqualTo(2);
    assertThat(movements.count()).isEqualTo(1);
  }

  @Test
  void duplicateSkuIsCaseInsensitive() {
    product("abc", 2);
    assertThatThrownBy(() -> product("ABC", 2)).isInstanceOf(InventoryService.Conflict.class);
  }

  @Test
  void oversizedOrderTotalIsRejectedWithoutDeductingStock() {
    Product p =
        service.addProduct(
            new Requests.ProductInput(
                "HIGH", "High value item", "Equipment", new BigDecimal("9999999999.99"), 1000, 2),
            "admin");
    assertThatThrownBy(() -> service.placeOrder(order(p.id, 101), "staff"))
        .isInstanceOf(InventoryService.Conflict.class);
    assertThat(products.findById(p.id).orElseThrow().stock).isEqualTo(1000);
    assertThat(orders.count()).isZero();
  }

  @Test
  void duplicateOrderLinesRejectedWithoutChangingStock() {
    Product p = product("A", 5);
    assertThatThrownBy(
            () ->
                service.placeOrder(
                    new Requests.OrderInput(
                        "Customer",
                        List.of(new Requests.ItemInput(p.id, 1), new Requests.ItemInput(p.id, 1))),
                    "staff"))
        .isInstanceOf(InventoryService.Conflict.class);
    assertThat(products.findById(p.id).orElseThrow().stock).isEqualTo(5);
  }

  @Test
  void orderPriceIsSnapshotOfOriginalPrice() {
    Product p = product("A", 5);
    SalesOrder o = service.placeOrder(order(p.id, 1), "staff");
    service.updateProduct(
        p.id,
        new Requests.ProductInput("A", "Renamed", "Accessories", new BigDecimal("30.00"), 4, 2),
        "admin");
    SalesOrder saved = orders.findById(o.id).orElseThrow();
    assertThat(saved.items.get(0).unitPrice).isEqualByComparingTo("19.95");
    assertThat(saved.items.get(0).productName).isEqualTo("Test product");
  }

  @Test
  void simultaneousOrdersCannotOversell() throws Exception {
    Product p = product("A", 1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
    Callable<Boolean> buy =
        () -> {
          ready.countDown();
          start.await();
          try {
            service.placeOrder(order(p.id, 1), "staff");
            return true;
          } catch (InventoryService.Conflict e) {
            return false;
          }
        };
    try {
      Future<Boolean> a = pool.submit(buy), b = pool.submit(buy);
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      int successes = (a.get(15, TimeUnit.SECONDS) ? 1 : 0) + (b.get(15, TimeUnit.SECONDS) ? 1 : 0);
      assertThat(successes).isEqualTo(1);
      assertThat(products.findById(p.id).orElseThrow().stock).isZero();
      assertThat(orders.count()).isEqualTo(1);
    } finally {
      start.countDown();
      pool.shutdownNow();
    }
  }

  @Test
  void simultaneousCancellationsRestoreOnlyOnce() throws Exception {
    Product p = product("A", 4);
    SalesOrder o = service.placeOrder(order(p.id, 2), "staff");
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    Callable<Void> cancel =
        () -> {
          start.await();
          service.cancel(o.id, "staff");
          return null;
        };
    try {
      Future<Void> a = pool.submit(cancel), b = pool.submit(cancel);
      start.countDown();
      a.get(15, TimeUnit.SECONDS);
      b.get(15, TimeUnit.SECONDS);
      assertThat(products.findById(p.id).orElseThrow().stock).isEqualTo(4);
      assertThat(movements.count()).isEqualTo(3);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void unauthenticatedApiRequiresLogin() throws Exception {
    mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void staffCannotAdjustStock() throws Exception {
    Product p = product("A", 2);
    mvc.perform(
            post("/api/products/" + p.id + "/stock")
                .with(csrf())
                .contentType("application/json")
                .content("{\"delta\":1,\"reason\":\"Delivery\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void mutationRequiresCsrf() throws Exception {
    mvc.perform(post("/api/products").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void invalidInputRejected() throws Exception {
    mvc.perform(
            post("/api/products")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"sku\":\"A\",\"name\":\"Test\",\"category\":\"Test\",\"price\":-1,\"stock\":0,\"lowStockThreshold\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void staffCanCreateOrder() throws Exception {
    Product p = product("A", 2);
    mvc.perform(
            post("/api/orders")
                .with(csrf())
                .contentType("application/json")
                .content(
                    "{\"customer\":\"Buyer\",\"items\":[{\"productId\":"
                        + p.id
                        + ",\"quantity\":1}]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("total").value(19.95));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void dashboardExcludesCancelledRevenue() throws Exception {
    Product p = product("A", 5);
    SalesOrder o = service.placeOrder(order(p.id, 2), "staff");
    service.cancel(o.id, "staff");
    mvc.perform(get("/api/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("revenue").value(0))
        .andExpect(jsonPath("confirmedOrders").value(0));
  }

  @Test
  void realPasswordLoginSucceeds() throws Exception {
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "admin")
                .param("password", "TestAdminPassword123"))
        .andExpect(status().isNoContent());
  }

  @Test
  void incorrectPasswordLoginFails() throws Exception {
    mvc.perform(post("/login").with(csrf()).param("username", "admin").param("password", "wrong"))
        .andExpect(status().isUnauthorized());
  }
}
