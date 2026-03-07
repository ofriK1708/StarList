package model.domain;

import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitCompletion {

    private Long id;
    private Long habitId;
    private Long userId;
    private LocalDate completedDate;
    private Integer coinsEarned;
    private Integer streakAtCompletion;
    private Instant createdAt;
}
