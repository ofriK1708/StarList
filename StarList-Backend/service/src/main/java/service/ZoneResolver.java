package service;

import java.time.ZoneId;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves a client-supplied IANA timezone string to a {@link ZoneId}.
 *
 * <p>"Today" decides whether a habit streak survives, so every date computation in the habit
 * flow must agree on a single zone. This is the one place that mapping happens.
 */
@Slf4j
public final class ZoneResolver {

    private ZoneResolver() {
    }

    /**
     * @param timezone an IANA zone id such as {@code "Asia/Jerusalem"}
     * @return the matching zone, or {@link ZoneOffset#UTC} when the input is absent or unparseable
     */
    public static ZoneId resolve(String timezone) {
        if (timezone == null || timezone.isBlank()) return ZoneOffset.UTC;
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            log.warn("Invalid timezone '{}', falling back to UTC", timezone);
            return ZoneOffset.UTC;
        }
    }
}
