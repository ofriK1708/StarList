package repository.api;

import org.springframework.data.jpa.repository.JpaRepository;
import repository.entity.CoinTransactionEntity;

public interface CoinTransactionRepository extends JpaRepository<CoinTransactionEntity, Long> {}
