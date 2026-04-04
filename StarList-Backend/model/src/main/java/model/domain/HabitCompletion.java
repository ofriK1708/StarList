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

    @Builder.Default
    private LocalDate completedDate = LocalDate.now();

    @Builder.Default
    private Integer coinsEarned = 0;

    @Builder.Default
    private Integer streakAtCompletion = 0;

    @Builder.Default
    private Instant createdAt = Instant.now();
}