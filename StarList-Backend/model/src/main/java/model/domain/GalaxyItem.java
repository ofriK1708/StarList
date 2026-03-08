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
    private Instant purchaseDate;
    private Integer timesSelected;
    private Boolean isActive;
}
