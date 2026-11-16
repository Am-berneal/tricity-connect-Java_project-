package com.tricityconnect.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.tricityconnect.model.Booking;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

/** Creates the downloadable PDF e-ticket. */
@Service
public class TicketPdfService {

    private static final Color BLUE = new Color(0x1E, 0x3A, 0x8A);
    private static final Color ORANGE = new Color(0xF9, 0x73, 0x16);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a");

    private final QrService qrService;

    public TicketPdfService(QrService qrService) {
        this.qrService = qrService;
    }

    public byte[] ticket(Booking b) {
        try {
            return buildTicket(b);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create the PDF ticket", e);
        }
    }

    private byte[] buildTicket(Booking b) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A5.rotate(), 30, 30, 25, 25);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BLUE);
        Font label = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        Font value = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.BLACK);
        Font big = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, ORANGE);

        try (InputStream logo = new ClassPathResource("static/images/logo.png").getInputStream()) {
            Image img = Image.getInstance(logo.readAllBytes());
            img.scaleToFit(140, 45);
            doc.add(img);
        } catch (IOException e) {
            doc.add(new Paragraph("TriCity Connect", title));
        }


        doc.add(new Paragraph("E-Ticket  •  PNR " + b.getPnr(), title));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(new float[]{3, 3, 2.2f});
        table.setWidthPercentage(100);

        String route = b.getTrip().getRoute().getCode() + " – " + b.getTrip().getRoute().getName();
        table.addCell(cell("ROUTE", route, label, value));
        table.addCell(cell("BUS", b.getTrip().getBus().getRegNo() + " (" + b.getTrip().getBus().getType().getLabel() + ")", label, value));

        Image qr = Image.getInstance(qrService.png(qrText(b), 300));
        qr.scaleToFit(120, 120);
        PdfPCell qrCell = new PdfPCell(qr);
        qrCell.setRowspan(4);
        qrCell.setBorder(Rectangle.NO_BORDER);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(qrCell);

        table.addCell(cell("FROM", b.getFromStop().getStop().getName() + "\n" + TIME.format(b.getBoardingTime()), label, value));
        table.addCell(cell("TO", b.getToStop().getStop().getName() + "\n" + TIME.format(b.getDropTime()) + " (expected)", label, value));
        table.addCell(cell("DATE", DATE.format(b.getBoardingTime()), label, value));
        table.addCell(cell("SEAT", b.getSeatNo(), label, big));
        table.addCell(cell("PASSENGER", b.getPassengerName() + ", " + b.getPassengerAge() + " (" + b.getPassengerGender().getLabel() + ")", label, value));
        table.addCell(cell("AMOUNT PAID", "Rs. " + b.getTotalFare() + "  via " + b.getPaymentMethod().getLabel()
                + "\n(Base " + b.getBaseFare() + " + CGST " + b.getCgst() + " + SGST " + b.getSgst() + ")", label, value));
        doc.add(table);

        doc.add(new Paragraph(" "));
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
        doc.add(new Paragraph("Status: " + b.getStatus() + ".  Show this QR to the conductor. Please reach your stop 5 minutes early. "
                + "Your seat is reserved only from your boarding stop to your drop stop. "
                + "Helpline 1800-TRICITY  •  Emergency 112", small));
        doc.close();
        return out.toByteArray();
    }

    public static String qrText(Booking b) {
        return "TRICITY|" + b.getPnr() + "|" + b.getTrip().getId() + "|" + b.getSeatNo() + "|"
                + b.getFromSeq() + "-" + b.getToSeq();
    }

    private PdfPCell cell(String heading, String text, Font label, Font value) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(heading + "\n", label));
        p.add(new Chunk(text, value));
        PdfPCell c = new PdfPCell(p);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingBottom(8);
        return c;
    }
}
