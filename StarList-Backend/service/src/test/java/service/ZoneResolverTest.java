package service;

import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneResolverTest {

    @Test
    void resolve_validIanaZone_returnsThatZone() {
        assertThat(ZoneResolver.resolve("Asia/Jerusalem")).isEqualTo(ZoneId.of("Asia/Jerusalem"));
    }

    @Test
    void resolve_null_returnsUtc() {
        assertThat(ZoneResolver.resolve(null)).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void resolve_blank_returnsUtc() {
        assertThat(ZoneResolver.resolve("   ")).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void resolve_unrecognisedZone_returnsUtc() {
        assertThat(ZoneResolver.resolve("Not/AZone")).isEqualTo(ZoneOffset.UTC);
    }
}
