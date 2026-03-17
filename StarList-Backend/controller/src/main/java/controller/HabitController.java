package controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.HabitService;
import service.dto.AddHabitRequest;
import service.dto.AddHabitResponse;
import service.dto.HabitResponse;
import service.dto.MarkHabitDoneResponse;
import service.dto.UpdateHabitRequest;

@Validated
@RestController
@RequestMapping("/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @PostMapping
    public ResponseEntity<AddHabitResponse> addHabit(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddHabitRequest request) {
        AddHabitResponse result = habitService.addHabit(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{habitId}")
    public ResponseEntity<HabitResponse> getHabit(
            @PathVariable Long habitId,
            @RequestParam(required = false) @Min(2000) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month) {
        return ResponseEntity.ok(habitService.getHabit(habitId, resolveYearMonth(year, month)));
    }

    @GetMapping
    public ResponseEntity<List<HabitResponse>> getUserHabits(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) @Min(2000) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month) {
        return ResponseEntity.ok(habitService.getUserHabits(userId, resolveYearMonth(year, month)));
    }

    /** Returns the provided {@link YearMonth}, or the current month if either param is absent. */
    private static YearMonth resolveYearMonth(Integer year, Integer month) {
        if (year == null || month == null) return YearMonth.now();
        return YearMonth.of(year, month);
    }

    @PutMapping("/{habitId}")
    public ResponseEntity<HabitResponse> updateHabit(
            @PathVariable Long habitId,
            @Valid @RequestBody UpdateHabitRequest request) {
        return ResponseEntity.ok(habitService.updateHabit(habitId, request));
    }

    @PostMapping("/{habitId}/complete")
    public ResponseEntity<MarkHabitDoneResponse> completeHabit(@PathVariable Long habitId) {
        return ResponseEntity.ok(habitService.completeHabit(habitId));
    }

    @DeleteMapping("/{habitId}")
    public ResponseEntity<Void> deleteHabit(@PathVariable Long habitId) {
        habitService.deleteHabit(habitId);
        return ResponseEntity.noContent().build();
    }
}
