package app.powerzone.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.powerzone.billing.dto.MembershipResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    private static final long GYM = 1L;
    private static final long USER = 5L;
    private static final long PLAN = 3L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    @Mock
    private MembershipRepository membershipRepository;

    private MembershipService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-20T15:00:00Z"), ZoneOffset.UTC);
        service = new MembershipService(membershipRepository, clock, 7);
    }

    @Test
    @DisplayName("sin registro devuelve NONE y cero dias restantes")
    void returnsNoneWithoutMembership() {
        when(membershipRepository.findByUser(GYM, USER)).thenReturn(Optional.empty());

        MembershipResponse response = service.status(GYM, USER);

        assertThat(response.state()).isEqualTo(MembershipState.NONE);
        assertThat(response.planName()).isNull();
        assertThat(response.daysRemaining()).isZero();
        assertThat(response.expiringThresholdDays()).isEqualTo(7);
    }

    @Test
    @DisplayName("una membresia con mas del umbral de dias esta ACTIVE")
    void returnsActiveWhenFarFromExpiry() {
        when(membershipRepository.findByUser(GYM, USER))
                .thenReturn(Optional.of(membership(TODAY.minusDays(20), TODAY.plusDays(8))));

        MembershipResponse response = service.status(GYM, USER);

        assertThat(response.state()).isEqualTo(MembershipState.ACTIVE);
        assertThat(response.daysRemaining()).isEqualTo(8);
        assertThat(response.planName()).isEqualTo("Mensual");
    }

    @Test
    @DisplayName("exactamente en el umbral la membresia es EXPIRING")
    void returnsExpiringAtThreshold() {
        when(membershipRepository.findByUser(GYM, USER))
                .thenReturn(Optional.of(membership(TODAY.minusDays(20), TODAY.plusDays(7))));

        MembershipResponse response = service.status(GYM, USER);

        assertThat(response.state()).isEqualTo(MembershipState.EXPIRING);
        assertThat(response.daysRemaining()).isEqualTo(7);
    }

    @Test
    @DisplayName("un dia antes del vencimiento sigue en EXPIRING")
    void returnsExpiringOneDayLeft() {
        when(membershipRepository.findByUser(GYM, USER))
                .thenReturn(Optional.of(membership(TODAY.minusDays(20), TODAY.plusDays(1))));

        assertThat(service.status(GYM, USER).state()).isEqualTo(MembershipState.EXPIRING);
    }

    @Test
    @DisplayName("el dia de vencimiento se considera EXPIRED (ends_at exclusivo)")
    void treatsEndDateAsExclusive() {
        when(membershipRepository.findByUser(GYM, USER))
                .thenReturn(Optional.of(membership(TODAY.minusDays(30), TODAY)));

        MembershipResponse response = service.status(GYM, USER);

        assertThat(response.state()).isEqualTo(MembershipState.EXPIRED);
        assertThat(response.daysRemaining()).isZero();
        assertThat(response.planName()).isEqualTo("Mensual");
    }

    @Test
    @DisplayName("una membresia vencida hace dias queda EXPIRED sin dias restantes")
    void returnsExpiredInThePast() {
        when(membershipRepository.findByUser(GYM, USER))
                .thenReturn(Optional.of(membership(TODAY.minusDays(60), TODAY.minusDays(30))));

        MembershipResponse response = service.status(GYM, USER);

        assertThat(response.state()).isEqualTo(MembershipState.EXPIRED);
        assertThat(response.daysRemaining()).isZero();
    }

    @Test
    @DisplayName("activa una membresia nueva desde hoy")
    void activatesNewMembership() {
        Membership updated = membership(TODAY, TODAY.plusDays(30));
        when(membershipRepository.findByUser(GYM, USER))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(updated));

        Membership result = service.activateOrRenew(GYM, USER, PLAN, 30);

        verify(membershipRepository).upsert(GYM, USER, PLAN, TODAY, TODAY.plusDays(30));
        assertThat(result.startsAt()).isEqualTo(TODAY);
        assertThat(result.endsAt()).isEqualTo(TODAY.plusDays(30));
    }

    @Test
    @DisplayName("renueva sumando el periodo al vencimiento vigente")
    void renewsAfterCurrentEnd() {
        LocalDate currentEnd = TODAY.plusDays(12);
        Membership updated = membership(currentEnd, currentEnd.plusDays(30));
        when(membershipRepository.findByUser(GYM, USER))
                .thenReturn(Optional.of(membership(TODAY.minusDays(18), currentEnd)))
                .thenReturn(Optional.of(updated));

        Membership result = service.activateOrRenew(GYM, USER, PLAN, 30);

        verify(membershipRepository).upsert(GYM, USER, PLAN, currentEnd, currentEnd.plusDays(30));
        assertThat(result.startsAt()).isEqualTo(currentEnd);
    }

    @Test
    @DisplayName("reinicia la vigencia cuando la membresia ya vencio")
    void restartsAfterExpiry() {
        LocalDate expiredEnd = TODAY.minusDays(4);
        Membership updated = membership(TODAY, TODAY.plusDays(90));
        when(membershipRepository.findByUser(GYM, USER))
                .thenReturn(Optional.of(membership(TODAY.minusDays(94), expiredEnd)))
                .thenReturn(Optional.of(updated));

        service.activateOrRenew(GYM, USER, PLAN, 90);

        verify(membershipRepository).upsert(GYM, USER, PLAN, TODAY, TODAY.plusDays(90));
    }

    @Test
    @DisplayName("falla si no puede releer la membresia recien guardada")
    void failsWhenMembershipCannotBeReloaded() {
        when(membershipRepository.findByUser(GYM, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateOrRenew(GYM, USER, PLAN, 30))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("membresia actualizada");
    }

    @Test
    @DisplayName("expone el umbral configurado")
    void exposesThreshold() {
        assertThat(service.expiringDays()).isEqualTo(7);
    }

    private static Membership membership(LocalDate startsAt, LocalDate endsAt) {
        return new Membership(
                9L, GYM, USER, PLAN, "Mensual", startsAt, endsAt,
                Instant.parse("2026-11-20T15:00:00Z"));
    }
}
