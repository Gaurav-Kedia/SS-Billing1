package com.example.billing.service;

import com.example.billing.invoice.Invoice;
import java.net.URI;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final boolean enabled;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String baseUrl;
    private final RestTemplate restTemplate;

    public WhatsAppService(@Value("${app.whatsapp.enabled:false}") boolean enabled,
                           @Value("${app.whatsapp.account-sid:}") String accountSid,
                           @Value("${app.whatsapp.auth-token:}") String authToken,
                           @Value("${app.whatsapp.from-number:}") String fromNumber,
                           @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.enabled = enabled && !accountSid.isBlank() && !authToken.isBlank() && !fromNumber.isBlank();
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.restTemplate = new RestTemplate();
    }

    public void sendInvoice(Invoice invoice) {
        if (!enabled) {
            log.info("WhatsApp integration disabled. Skipping message for bill {}", invoice.getBillNumber());
            return;
        }
        try {
            String to = normalizePhone(invoice.getCustomerPhone());
            if (to == null) {
                log.warn("No customer phone found for invoice {}. Skipping WhatsApp message", invoice.getBillNumber());
                return;
            }

            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("To", to);
            body.add("From", fromNumber);
            body.add("Body", buildMessage(invoice));

            Optional<String> pdfUrl = buildPdfUrl(invoice);
            pdfUrl.ifPresent(value -> body.add("MediaUrl", value));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(accountSid, authToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(URI.create(url), request, String.class);
        } catch (Exception ex) {
            log.error("Failed to send WhatsApp message for invoice {}", invoice.getBillNumber(), ex);
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        if (!digits.startsWith("91") && digits.length() == 10) {
            digits = "91" + digits;
        }
        return "whatsapp:+" + digits;
    }

    private String buildMessage(Invoice invoice) {
        return "Dear " + invoice.getCustomerName() + ", your invoice " + invoice.getBillNumber()
                + " amounting to Rs." + invoice.getTotalAmount()
                + " has been generated. Thank you for your business.";
    }

    private Optional<String> buildPdfUrl(Invoice invoice) {
        if (invoice.getId() == null) {
            return Optional.empty();
        }
        String url = baseUrl + "/invoices/" + invoice.getId() + "/pdf";
        return Optional.of(url);
    }
}
