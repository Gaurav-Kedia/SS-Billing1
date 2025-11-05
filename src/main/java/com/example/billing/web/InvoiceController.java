package com.example.billing.web;

import com.example.billing.catalog.HsnCodeCatalog;
import com.example.billing.invoice.Invoice;
import com.example.billing.service.InvoiceService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final HsnCodeCatalog hsnCodeCatalog;

    public InvoiceController(InvoiceService invoiceService, HsnCodeCatalog hsnCodeCatalog) {
        this.invoiceService = invoiceService;
        this.hsnCodeCatalog = hsnCodeCatalog;
    }

    @GetMapping("/")
    public String invoiceForm(Model model) {
        InvoiceForm form = new InvoiceForm();
        form.setIssueDate(LocalDate.now());
        form.setDeliveryDate(LocalDate.now());
        ensureEmptyItems(form);
        model.addAttribute("form", form);
        model.addAttribute("hsnMap", hsnCodeCatalog.getHsnToDescription());
        return "invoice/form";
    }

    @PostMapping("/invoices")
    public String createInvoice(@Valid @ModelAttribute("form") InvoiceForm form,
                                BindingResult bindingResult,
                                Model model) {
        boolean hasLineItem = form.getItems() != null && form.getItems().stream()
                .anyMatch(item -> item.getFinalAmount() != null && item.getFinalAmount().compareTo(BigDecimal.ZERO) > 0);
        if (!hasLineItem) {
            bindingResult.reject("items.empty", "Add at least one item with a final amount.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("hsnMap", hsnCodeCatalog.getHsnToDescription());
            return "invoice/form";
        }
        Invoice invoice = invoiceService.createInvoice(form);
        model.addAttribute("invoice", invoice);
        return "invoice/success";
    }

    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Invoice invoice = invoiceService.findById(id);
        byte[] pdf = invoiceService.createInvoicePdfBytes(invoice);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", invoice.getBillNumber() + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private void ensureEmptyItems(InvoiceForm form) {
        if (!form.getItems().isEmpty()) {
            return;
        }
        Map<String, String> hsnMap = hsnCodeCatalog.getHsnToDescription();
        List<InvoiceItemForm> items = new ArrayList<>();
        for (Map.Entry<String, String> entry : hsnMap.entrySet()) {
            InvoiceItemForm itemForm = new InvoiceItemForm();
            itemForm.setHsnCode(entry.getKey());
            itemForm.setDescription(entry.getValue());
            itemForm.setQuantity(1);
            items.add(itemForm);
        }
        if (items.isEmpty()) {
            items.add(new InvoiceItemForm());
        }
        form.setItems(items);
    }
}
