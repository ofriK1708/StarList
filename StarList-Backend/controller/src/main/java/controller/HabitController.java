package controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import service.HabitService;
import service.UserService;
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
    private final UserService userService;

    public HabitController(HabitService habitService, UserService userService) {
        this.habitService = habitService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<AddHabitResponse> addHabit(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddHabitRequest request) {
        Long userId = userService.getUserByCognitoSub(jwt.getSubject()).id();
        AddHabitResponse result = habitService.addHabit(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{habitId}")
    public ResponseEntity<HabitResponse> getHabit(
            @PathVariable Long habitId,
            @RequestParam(required = false) @Min(2000) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) String userTimezone) {
        return ResponseEntity.ok(habitService.getHabit(habitId, resolveYearMonth(year, month), userTimezone));
    }

    @GetMapping
    public ResponseEntity<List<HabitResponse>> getUserHabits(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) @Min(2000) Integer year,
            @RequestParam(required = false) @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) String userTimezone) {
        Long userId = userService.getUserByCognitoSub(jwt.getSubject()).id();
        return ResponseEntity.ok(habitService.getUserHabits(userId, resolveYearMonth(year, month), userTimezone));
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

    /**
     * @param userTimezone IANA zone id from the client (e.g. "Asia/Jerusalem"). Decides which
     *                     calendar day the completion lands on, and therefore whether it is late.
     *                     Absent or unparseable values fall back to UTC.
     */
    @PostMapping("/{habitId}/complete")
    public ResponseEntity<MarkHabitDoneResponse> completeHabit(
            @PathVariable Long habitId,
            @RequestParam(required = false) String userTimezone) {
        return ResponseEntity.ok(habitService.completeHabit(habitId, userTimezone));
    }

    @DeleteMapping("/{habitId}")
    public ResponseEntity<Void> deleteHabit(@PathVariable Long habitId) {
        habitService.deleteHabit(habitId);
        return ResponseEntity.noContent().build();
    }
}
