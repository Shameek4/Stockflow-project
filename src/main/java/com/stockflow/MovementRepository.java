package com.stockflow;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository extends JpaRepository<StockMovement, Long> {
  List<StockMovement> findTop100ByOrderByIdDesc();
}
