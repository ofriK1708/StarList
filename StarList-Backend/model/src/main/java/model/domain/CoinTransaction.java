package model.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.enums.ReferenceType;
import model.enums.TransactionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinTransaction {

    private Long id;
    private Long userId;

    @Builder.Default
    private Integer amount = 0;

    private TransactionType transactionType;
    private ReferenceType referenceType;
    private Long referenceId;
    private String description;

    @Builder.Default
    private Instant createdAt = Instant.now();
}