package app.powerzone.billing.receipt;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReceiptPdfRendererTest {

    private final ReceiptPdfRenderer renderer = new ReceiptPdfRenderer();

    @Test
    @DisplayName("genera un PDF con cabecera valida")
    void rendersPdf() {
        byte[] pdf = renderer.render(data());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
        assertThat(pdf.length).isGreaterThan(500);
    }

    @Test
    @DisplayName("tolera campos opcionales nulos")
    void toleratesNullFields() {
        ReceiptData data = new ReceiptData(
                null, null, null, 1L, Instant.parse("2026-11-20T15:00:00Z"),
                null, 9_900, "COP", null, "COMPLETED");

        assertThatCode(() -> renderer.render(data)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("formatea el monto con la convencion colombiana")
    void formatsMoney() {
        String formatted = ReceiptPdfRenderer.money(9_900, "COP");

        assertThat(formatted).contains("99");
        assertThat(formatted).contains("COP");
    }

    @Test
    @DisplayName("formatea montos de varios millones con separador de miles")
    void formatsLargeAmounts() {
        String formatted = ReceiptPdfRenderer.money(150_000_000, "COP");

        assertThat(formatted).contains("1.500.000");
        assertThat(formatted).endsWith("COP");
    }

    private static ReceiptData data() {
        return new ReceiptData(
                "PowerZone Centro", "Ana Gomez", "ana@powerzone.app", 42L,
                Instant.parse("2026-11-20T15:00:00Z"), "Mensual", 9_900, "COP",
                "Tarjeta", "COMPLETED");
    }
}
