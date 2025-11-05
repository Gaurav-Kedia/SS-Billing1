package com.example.billing.service;

import com.example.billing.invoice.Invoice;
import com.example.billing.invoice.InvoiceItem;
import com.example.billing.invoice.InvoiceRepository;
import com.example.billing.web.InvoiceForm;
import com.example.billing.web.InvoiceItemForm;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final BigDecimal GST_RATE = new BigDecimal("0.18");
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal TWO = new BigDecimal("2");

    private final InvoiceRepository invoiceRepository;
    private final PdfService pdfService;
    private final WhatsAppService whatsAppService;
    private final PrintService printService;
    private final Path pdfStorage;

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    public InvoiceService(InvoiceRepository invoiceRepository,
                          PdfService pdfService,
                          WhatsAppService whatsAppService,
                          PrintService printService,
                          @Value("${app.pdf-storage:src/main/resources/files}") String pdfStorageDir) throws IOException {
        this.invoiceRepository = invoiceRepository;
        this.pdfService = pdfService;
        this.whatsAppService = whatsAppService;
        this.printService = printService;
        this.pdfStorage = Path.of(pdfStorageDir).toAbsolutePath();
        Files.createDirectories(this.pdfStorage);
    }

    @Transactional
    public Invoice createInvoice(InvoiceForm form) {
        Invoice invoice = new Invoice();
        invoice.setCustomerName(form.getCustomerName());
        invoice.setCustomerAddress(form.getCustomerAddress());
        invoice.setCustomerPhone(form.getCustomerPhone());
        invoice.setCustomerGstin(form.getCustomerGstin());
        invoice.setIssueDate(form.getIssueDate());
        invoice.setDeliveryDate(form.getDeliveryDate());
        invoice.setVehicleNumber(form.getVehicleNumber());
        invoice.setPlaceOfSupply(form.getPlaceOfSupply());

        BigDecimal discount = form.getDiscountAmount() != null ? form.getDiscountAmount() : BigDecimal.ZERO;
        invoice.setDiscountAmount(discount);

        invoice.clearItems();

        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalFinal = BigDecimal.ZERO;

        for (InvoiceItemForm itemForm : form.getItems()) {
            if (itemForm.getFinalAmount() == null) {
                continue;
            }
            InvoiceItem item = new InvoiceItem();
            item.setDescription(itemForm.getDescription());
            item.setHsnCode(itemForm.getHsnCode());
            int quantityValue = itemForm.getQuantity() != null ? itemForm.getQuantity() : 1;
            item.setQuantity(quantityValue);

            BigDecimal finalAmountPerUnit = itemForm.getFinalAmount();
            BigDecimal quantity = new BigDecimal(quantityValue);

            BigDecimal totalFinalAmount = finalAmountPerUnit.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalBaseAmount = totalFinalAmount.divide(ONE.add(GST_RATE), 2, RoundingMode.HALF_UP);
            BigDecimal totalTaxAmount = totalFinalAmount.subtract(totalBaseAmount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal sgst = totalTaxAmount.divide(TWO, 2, RoundingMode.HALF_UP);
            BigDecimal cgst = totalTaxAmount.subtract(sgst).setScale(2, RoundingMode.HALF_UP);

            item.setFinalAmount(totalFinalAmount);
            item.setBaseAmount(totalBaseAmount);
            item.setSgstAmount(sgst);
            item.setCgstAmount(cgst);

            invoice.addItem(item);

            totalBase = totalBase.add(totalBaseAmount);
            totalTax = totalTax.add(sgst).add(cgst);
            totalFinal = totalFinal.add(totalFinalAmount);
        }

        totalFinal = totalFinal.subtract(discount != null ? discount : BigDecimal.ZERO);
        if (totalFinal.compareTo(BigDecimal.ZERO) < 0) {
            totalFinal = BigDecimal.ZERO;
        }

        invoice.setTotalBaseAmount(totalBase.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalAmount(totalFinal.setScale(2, RoundingMode.HALF_UP));

        Invoice saved = invoiceRepository.save(invoice);
        if (saved.getBillNumber() == null) {
            String billNumber = String.format("SS-%05d", saved.getId());
            saved.setBillNumber(billNumber);
            saved = invoiceRepository.save(saved);
        }

        try {
            byte[] pdfBytes = pdfService.renderInvoicePdf(saved);
            Path pdfPath = storePdf(saved, pdfBytes);
            whatsAppService.sendInvoice(saved);
            printService.printInvoice(pdfPath);
        } catch (Exception e) {
            log.error("Failed to post-process invoice {}", saved.getId(), e);
        }

        return saved;
    }

    private Path storePdf(Invoice invoice, byte[] pdfBytes) throws IOException {
        String safeBillNumber = invoice.getBillNumber() != null ? invoice.getBillNumber() : "invoice-" + invoice.getId();
        Path pdfPath = pdfStorage.resolve(safeBillNumber + ".pdf");
        Files.write(pdfPath, pdfBytes);
        return pdfPath;
    }

    public Invoice findById(Long id) {
        return invoiceRepository.findById(id).orElseThrow();
    }

    public byte[] createInvoicePdfBytes(Invoice invoice) {
        return pdfService.renderInvoicePdf(invoice);
    }

    public String formatDateForDisplay(Invoice invoice) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return invoice.getIssueDate() != null ? invoice.getIssueDate().format(formatter) : "";
    }
}
