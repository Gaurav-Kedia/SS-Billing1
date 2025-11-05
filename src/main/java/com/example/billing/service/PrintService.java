package com.example.billing.service;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PrintService {

    private static final Logger log = LoggerFactory.getLogger(PrintService.class);

    public void printInvoice(Path pdfPath) {
        if (pdfPath == null) {
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            log.warn("Desktop printing not supported on this environment. Skipping print for {}", pdfPath);
            return;
        }
        try {
            Desktop.getDesktop().print(pdfPath.toFile());
        } catch (IOException e) {
            log.error("Failed to print invoice {}", pdfPath, e);
        }
    }
}
