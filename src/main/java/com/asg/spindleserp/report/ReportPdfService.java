package com.asg.spindleserp.report;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReportPdfService — thin JasperReports wrapper for PDF generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportPdfService {

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, JasperReport> cache = new ConcurrentHashMap<>();

    public Map<String, Object> brandingParams(Long orgId) {
        Map<String, Object> p = new HashMap<>();
        try {
            Map<String, Object> org = jdbcTemplate.queryForMap(
                "SELECT name, display_name, address, phone, email, website, logo_url FROM organizations WHERE id = ?", orgId);
            p.putAll(org);
            p.put("orgId", orgId);
        } catch (Exception e) {
            p.put("org_name", "Spindle ERP");
            p.put("orgId", orgId);
        }
        return p;
    }

    public byte[] fillToPdf(String jrxml, Map<String, Object> params) {
        try {
            JasperReport report = cache.computeIfAbsent(jrxml, this::compile);
            JasperPrint print = JasperFillManager.fillReport(report, params, new JREmptyDataSource());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF failed: " + jrxml, e);
        }
    }

    private JasperReport compile(String jrxml) {
        try (InputStream is = getClass().getResourceAsStream("/reports/" + jrxml + ".jrxml")) {
            if (is == null) throw new RuntimeException("Not found: " + jrxml);
            return JasperCompileManager.compileReport(is);
        } catch (Exception e) {
            throw new RuntimeException("Compile failed: " + jrxml, e);
        }
    }

    public ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_PDF);
        h.setContentDispositionFormData("inline", filename);
        h.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(h).body(pdf);
    }
}
