package app.powerzone.billing.receipt;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Genera el comprobante informativo en PDF con OpenPDF. */
@Component
public class ReceiptPdfRenderer {

    private static final Color BRAND = new Color(208, 48, 48);
    private static final Color INK = new Color(33, 30, 27);
    private static final Color MUTED = new Color(110, 98, 88);
    private static final DateTimeFormatter DATE = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm", new Locale("es", "CO"))
            .withZone(ZoneOffset.UTC);

    public byte[] render(ReceiptData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 54, 54, 54, 54);
            PdfWriter.getInstance(document, out);
            document.open();

            Font brand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BRAND);
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, INK);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10, INK);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED);

            Paragraph header = new Paragraph("PowerZone", brand);
            header.setSpacingAfter(2);
            document.add(header);

            Paragraph gym = new Paragraph(data.gymName(), body);
            gym.setSpacingAfter(18);
            document.add(gym);

            Paragraph heading = new Paragraph(
                    "Comprobante de pago No. " + data.number(), title);
            heading.setSpacingAfter(4);
            document.add(heading);

            Paragraph issued = new Paragraph(
                    "Emitido el " + DATE.format(data.issuedAt()) + " (UTC)", small);
            issued.setSpacingAfter(18);
            document.add(issued);

            PdfPTable table = new PdfPTable(new float[]{1.1f, 2f});
            table.setWidthPercentage(100);
            addRow(table, "Cliente", data.userName(), body);
            addRow(table, "Correo", data.userEmail(), body);
            addRow(table, "Concepto", data.planName(), body);
            addRow(table, "Monto", money(data.amountCents(), data.currency()), body);
            addRow(table, "Metodo de pago", data.methodLabel(), body);
            addRow(table, "Estado", data.status(), body);
            document.add(table);

            Paragraph note = new Paragraph(
                    "Este comprobante es informativo y no constituye una factura electronica "
                            + "con validez fiscal. Conservalo como soporte de tu pago.",
                    small);
            note.setSpacingBefore(24);
            document.add(note);

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new IllegalStateException("No se pudo generar el comprobante PDF", ex);
        }
    }

    private static void addRow(PdfPTable table, String label, String value, Font body) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, body));
        labelCell.setBorder(0);
        labelCell.setPaddingBottom(8);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value == null ? "—" : value, body));
        valueCell.setBorder(0);
        valueCell.setPaddingBottom(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(valueCell);
    }

    static String money(int amountCents, String currency) {
        return String.format(new Locale("es", "CO"), "%,.0f %s",
                amountCents / 100.0, currency);
    }
}
