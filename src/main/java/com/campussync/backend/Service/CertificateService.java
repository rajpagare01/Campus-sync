package com.campussync.backend.Service;

import com.campussync.backend.Model.Event;
import com.campussync.backend.Model.User;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Service
public class CertificateService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US);

    private static final int PAGE_WIDTH = 842;
    private static final int PAGE_HEIGHT = 595;

    public CompletableFuture<byte[]> generateCertificate(Event event, User user) {
        try {
            return CompletableFuture.completedFuture(buildCertificatePdf(event, user));
        } catch (Exception ex) {
            CompletableFuture<byte[]> future = new CompletableFuture<>();
            future.completeExceptionally(ex);
            return future;
        }
    }

    private byte[] buildCertificatePdf(Event event, User user) {
        String attendeeName = safePdfText(user.getName(), "Participant");
        String eventTitle = safePdfText(event.getTitle(), "CampusSync Event");
        String eventDate = event.getDate() == null
                ? "Date to be announced"
                : safePdfText(event.getDate().format(DATE_FORMATTER), "Date to be announced");

        StringBuilder content = new StringBuilder();

        // Border
        content.append("q\n");
        content.append("2 w\n");
        content.append("50 50 742 495 re S\n");
        content.append("Q\n");

        addCenteredText(content, "F1", 28, 520, "Certificate of Participation");
        addCenteredText(content, "F2", 18, 455, "This certificate is proudly presented to");
        addCenteredText(content, "F1", 24, 390, attendeeName);
        addCenteredText(content, "F2", 16, 330, "For attending the event");
        addCenteredText(content, "F1", 20, 295, eventTitle);

        int y = 255;
        for (String line : wrapText("Held on " + eventDate, 48)) {
            addCenteredText(content, "F2", 16, y, line);
            y -= 24;
        }

        addCenteredText(content, "F2", 18, 135, "CampusSync");
        addCenteredText(content, "F2", 12, 105, "Generated automatically by CampusSync");

        return writePdf(content.toString());
    }

    private byte[] writePdf(String contentStream) {
        byte[] contentBytes = contentStream.getBytes(StandardCharsets.US_ASCII);
        List<byte[]> objects = new ArrayList<>();
        objects.add(pdfObject(1, "<< /Type /Catalog /Pages 2 0 R >>"));
        objects.add(pdfObject(2, "<< /Type /Pages /Count 1 /Kids [3 0 R] >>"));
        objects.add(pdfObject(
                3,
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT + "] "
                        + "/Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>"
        ));
        objects.add(pdfObject(4, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));
        objects.add(pdfObject(5, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        objects.add(pdfStreamObject(6, contentBytes));

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);

            for (byte[] object : objects) {
                offsets.add(out.size());
                out.write(object);
            }

            int xrefStart = out.size();
            out.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
            out.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
            for (int i = 1; i < offsets.size(); i++) {
                out.write(String.format(Locale.US, "%010d 00000 n \n", offsets.get(i))
                        .getBytes(StandardCharsets.US_ASCII));
            }
            out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.write(("startxref\n" + xrefStart + "\n%%EOF").getBytes(StandardCharsets.US_ASCII));
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate certificate PDF", ex);
        }
    }

    private byte[] pdfObject(int number, String body) {
        String object = number + " 0 obj\n" + body + "\nendobj\n";
        return object.getBytes(StandardCharsets.US_ASCII);
    }

    private byte[] pdfStreamObject(int number, byte[] contentBytes) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write((number + " 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.write(contentBytes);
            out.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build PDF stream", ex);
        }
    }

    private void addCenteredText(StringBuilder content, String fontName, int fontSize, int y, String text) {
        int x = centeredX(text, fontSize);
        content.append("BT\n")
                .append("/").append(fontName).append(" ").append(fontSize).append(" Tf\n")
                .append("1 0 0 1 ").append(x).append(" ").append(y).append(" Tm\n")
                .append("(").append(escapePdfText(text)).append(") Tj\n")
                .append("ET\n");
    }

    private int centeredX(String text, int fontSize) {
        int estimatedWidth = Math.max(120, (int) (text.length() * fontSize * 0.52));
        return Math.max(60, (PAGE_WIDTH - estimatedWidth) / 2);
    }

    private List<String> wrapText(String text, int maxLineLength) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.isEmpty()) {
                current.append(word);
                continue;
            }

            if (current.length() + 1 + word.length() > maxLineLength) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current.append(' ').append(word);
            }
        }

        if (!current.isEmpty()) {
            lines.add(current.toString());
        }

        return lines;
    }

    private String safePdfText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        StringBuilder sanitized = new StringBuilder();
        for (char ch : value.toCharArray()) {
            if (ch >= 32 && ch <= 126) {
                sanitized.append(ch);
            } else {
                sanitized.append('?');
            }
        }
        return sanitized.toString();
    }

    private String escapePdfText(String value) {
        return value.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}
