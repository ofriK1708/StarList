package service;

import lombok.extern.slf4j.Slf4j;
import model.enums.ReferenceType;
import model.enums.TransactionType;
import org.springframework.stereotype.Service;
import repository.api.CoinTransactionRepository;
import repository.entity.CoinTransactionEntity;
import repository.entity.UserEntity;

@Slf4j
@Service
public class CoinTransactionService {

    private final CoinTransactionRepository coinTransactionRepository;

    public CoinTransactionService(CoinTransactionRepository coinTransactionRepository) {
        this.coinTransactionRepository = coinTransactionRepository;
    }

    /**
     * Persists a {@link CoinTransactionEntity} for the given user and amount.
     *
     * @param amount positive = earned, negative = spent
     */
    public void record(UserEntity user, int amount, TransactionType type,
                       ReferenceType referenceType, Long referenceId, String description) {
        log.info("Recording coin transaction: {} coins for user {} ({})", amount, user.getId(), type);
        coinTransactionRepository.save(
                CoinTransactionEntity.builder()
                        .user(user)
                        .amount(amount)
                        .transactionType(type)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .description(description)
                        .build());
    }
}
