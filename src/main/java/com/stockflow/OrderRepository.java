package com.stockflow;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<SalesOrder, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from SalesOrder o where o.id=:id")
  Optional<SalesOrder> findLockedById(@Param("id") Long id);

  List<SalesOrder> findAllByOrderByIdDesc();
}
