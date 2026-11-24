package app.powerzone.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import app.powerzone.common.ApiException;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CheckoutRateLimiterTest {

    @Mock
    private Clock clock;

    private CheckoutRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new CheckoutRateLimiter(3, clock);
    }

    @Test
    @DisplayName("permite los intentos dentro del limite por usuario")
    void allowsAttemptsWithinLimit() {
        when(clock.millis()).thenReturn(1_000L);

        assertThatCode(() -> {
            limiter.check(5L);
            limiter.check(5L);
            limiter.check(5L);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("bloquea el intento que excede el limite")
    void blocksAttemptOverLimit() {
        when(clock.millis()).thenReturn(1_000L);
        limiter.check(5L);
        limiter.check(5L);
        limiter.check(5L);

        assertThatThrownBy(() -> limiter.check(5L))
                .isInstanceOf(ApiException.class)
                .satisfies(error -> {
                    ApiException api = (ApiException) error;
                    assertThat(api.code()).isEqualTo("CHECKOUT_RATE_LIMITED");
                    assertThat(api.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });
    }

    @Test
    @DisplayName("la ventana se libera despues de un minuto")
    void releasesWindowAfterOneMinute() {
        when(clock.millis()).thenReturn(1_000L);
        limiter.check(5L);
        limiter.check(5L);
        limiter.check(5L);

        when(clock.millis()).thenReturn(1_000L + 60_001L);

        assertThatCode(() -> limiter.check(5L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("los limites son independientes por usuario")
    void isolatesUsers() {
        when(clock.millis()).thenReturn(1_000L);
        limiter.check(5L);
        limiter.check(5L);
        limiter.check(5L);

        assertThatCode(() -> limiter.check(6L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("reset limpia las ventanas en memoria")
    void resetClearsWindows() {
        when(clock.millis()).thenReturn(1_000L);
        limiter.check(5L);
        limiter.check(5L);
        limiter.check(5L);

        limiter.reset();

        assertThatCode(() -> limiter.check(5L)).doesNotThrowAnyException();
    }
}
