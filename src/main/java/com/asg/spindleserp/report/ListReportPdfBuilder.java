package com.asg.spindleserp.report;

import lombok.Value;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Builds tabular PDFs dynamically using PDFBox — no JRXML required.
 */
@Component
public class ListReportPdfBuilder {

    private static final float MARGIN = 40;
    private static final float LINE_H = 12;

    private static final PDFont FONT    = PDType1Font.HELVETICA;
    private static final PDFont FONT_B  = PDType1Font.HELVETICA_BOLD;
    private static final int    FONT_SZ = 8;

    @Value
    public static class Col {
        String field;
        String header;
        float widthPct;
        String align;
        String format;
        boolean bold;
        String css;

        public static Col text(String f, String h, int pct) {
            return new Col(f, h, pct, "L", "text", false, null);
        }
        public static Col textC(String f, String h, int pct) {
            return new Col(f, h, pct, "C", "text", false, null);
        }
        public static Col money(String f, String h, int pct, boolean b) {
            return new Col(f, h, pct, "R", "money", b, null);
        }
        public static Col money2(String f, String h, int pct, boolean b) {
            return new Col(f, h, pct, "R", "money", b, null);
        }
        public static Col qty(String f, String h, int pct, boolean b) {
            return new Col(f, h, pct, "R", "qty", b, null);
        }
        public static Col qty3(String f, String h, int pct, boolean b) {
            return new Col(f, h, pct, "R", "qty3", b, null);
        }
        public Col withCss(String c) {
            return new Col(field, header, widthPct, align, format, bold, c);
        }
    }

    public byte[] build(String title, String subtitle,
                        List<String[]> headerKv,
                        List<Col> cols,
                        List<? extends Map<String, Object>> rows,
                        List<String[]> footerKv,
                        boolean landscape,
                        Map<String, Object> branding) {
        float pw = landscape ? PDRectangle.A4.getHeight() : PDRectangle.A4.getWidth();
        float ph = landscape ? PDRectangle.A4.getWidth()  : PDRectangle.A4.getHeight();
        float avail = pw - 2 * MARGIN;
        float maxY = MARGIN + 20;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(pw, ph));
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            float y = ph - MARGIN - 10;

            // Title
            if (title != null && !title.isEmpty()) {
                cs.setFont(FONT_B, 14);
                float tw = strWidth(FONT_B, 14, title);
                cs.beginText(); cs.newLineAtOffset((pw - tw) / 2f, y); cs.showText(title); cs.endText();
                y -= LINE_H + 4;
            }
            if (subtitle != null && !subtitle.isEmpty()) {
                cs.setFont(FONT, 9);
                float tw = strWidth(FONT, 9, subtitle);
                cs.beginText(); cs.newLineAtOffset((pw - tw) / 2f, y); cs.showText(subtitle); cs.endText();
                y -= LINE_H + 2;
            }

            // Header KV
            if (headerKv != null) {
                cs.setFont(FONT, FONT_SZ);
                for (String[] kv : headerKv) {
                    if (kv.length < 2) continue;
                    y = checkY(doc, cs, page, pw, ph, y, maxY);
                    cs.beginText(); cs.newLineAtOffset(MARGIN, y); cs.showText(kv[0] + ":  " + nvl(kv[1])); cs.endText();
                    y -= LINE_H - 2;
                }
                y -= 4;
            }

            // Table columns
            if (cols == null || cols.isEmpty()) {
                cs.close(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); doc.save(baos); return baos.toByteArray();
            }

            float totalPct = 0;
            for (Col c : cols) totalPct += c.widthPct;
            float[] widths = new float[cols.size()];
            for (int i = 0; i < cols.size(); i++) widths[i] = (cols.get(i).widthPct / totalPct) * avail;

            // Header row
            y = checkY(doc, cs, page, pw, ph, y, maxY + LINE_H);
            y = drawHeader(cs, cols, widths, MARGIN, y);
            y -= 2;

            // Data rows
            cs.setFont(FONT, FONT_SZ);
            for (Map<String, Object> row : rows) {
                y = checkY(doc, cs, page, pw, ph, y, maxY);
                float cx = MARGIN;
                for (int i = 0; i < cols.size(); i++) {
                    Col c = cols.get(i);
                    String val = fmtCell(c, row.get(c.field));
                    float cw = widths[i];
                    float tw = strWidth(c.isBold() ? FONT_B : FONT, FONT_SZ, val);
                    float tx = "R".equals(c.align) ? cx + cw - tw - 2 : "C".equals(c.align) ? cx + (cw - tw) / 2f : cx + 2;
                    cs.setFont(c.isBold() ? FONT_B : FONT, FONT_SZ);
                    cs.beginText(); cs.newLineAtOffset(tx, y); cs.showText(val); cs.endText();
                    cx += cw;
                }
                y -= LINE_H;
            }

            // Footer KV
            if (footerKv != null && !footerKv.isEmpty()) {
                y -= 6;
                cs.setFont(FONT_B, 9);
                for (String[] f : footerKv) {
                    if (f.length < 2) continue;
                    y = checkY(doc, cs, page, pw, ph, y, maxY);
                    cs.beginText(); cs.newLineAtOffset(MARGIN + avail * 0.5f, y); cs.showText(f[0] + "   " + nvl(f[1])); cs.endText();
                }
            }

            cs.close();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF build failed", e);
        }
    }

    private float drawHeader(PDPageContentStream cs, List<Col> cols, float[] widths, float x, float y) throws IOException {
        float cx = x;
        for (int i = 0; i < cols.size(); i++) {
            Col c = cols.get(i);
            float cw = widths[i];
            cs.setNonStrokingColor(0.85f, 0.85f, 0.85f);
            cs.addRect(cx, y - 1, cw, LINE_H + 2);
            cs.fill();
            cs.setNonStrokingColor(0, 0, 0);
            float tw = strWidth(FONT_B, FONT_SZ, c.header);
            float tx = "R".equals(c.align) ? cx + cw - tw - 2 : "C".equals(c.align) ? cx + (cw - tw) / 2f : cx + 2;
            cs.setFont(FONT_B, FONT_SZ);
            cs.beginText(); cs.newLineAtOffset(tx, y + 1); cs.showText(c.header); cs.endText();
            cx += cw;
        }
        return y - LINE_H - 2;
    }

    private float checkY(PDDocument doc, PDPageContentStream cs, PDPage page, float pw, float ph, float y, float maxY) throws IOException {
        if (y > maxY) return y;
        cs.close();
        page = new PDPage(new PDRectangle(pw, ph));
        doc.addPage(page);
        cs = new PDPageContentStream(doc, page);
        return ph - MARGIN - 10;
    }

    private static float strWidth(PDFont f, int sz, String s) throws IOException {
        return (f.getStringWidth(s) / 1000f) * sz;
    }

    private static String fmtCell(Col c, Object val) {
        String raw = val != null ? val.toString() : "";
        if ("money".equals(c.format) || "money2".equals(c.format)) {
            try { return String.format("%,.2f", new BigDecimal(raw)); } catch (Exception e) { return raw; }
        }
        if ("qty".equals(c.format) || "qty3".equals(c.format)) {
            try { return String.format("%,.3f", new BigDecimal(raw)); } catch (Exception e) { return raw; }
        }
        return raw;
    }

    private static String nvl(String s) { return s != null ? s : ""; }
}
