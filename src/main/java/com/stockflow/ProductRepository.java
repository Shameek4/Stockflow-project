package com.stockflow;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Product p where p.id=:id")
  Optional<Product> findLockedById(@Param("id") Long id);

  boolean existsBySku(String sku);

  List<Product> findAllByOrderByIdAsc();
}
