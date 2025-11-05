package com.example.billing.service;

import com.example.billing.invoice.Invoice;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);
    private final TemplateEngine templateEngine;

    public PdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] renderInvoicePdf(Invoice invoice) {
        try {
            Context context = new Context();
            context.setVariable("invoice", invoice);
            Map<String, Object> totals = new HashMap<>();
            totals.put("sgstRate", "9%");
            totals.put("cgstRate", "9%");
            totals.put("gstRate", "18%");
            context.setVariable("totals", totals);

            String html = templateEngine.process("invoice/pdf", context);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);
                builder.toStream(outputStream);
                builder.run();
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("Failed to generate invoice PDF", e);
            throw new IllegalStateException("Unable to generate invoice PDF", e);
        }
    }
}
