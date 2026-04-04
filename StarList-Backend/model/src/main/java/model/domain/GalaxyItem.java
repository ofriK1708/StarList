package model.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalaxyItem {

    private Long id;
    private Long userId;
    private Long catalogItemId;

    @Builder.Default
    private Instant purchaseDate = Instant.now();

    @Builder.Default
    private Integer timesSelected = 1;

    @Builder.Default
    private Boolean isActive = Boolean.TRUE;
}