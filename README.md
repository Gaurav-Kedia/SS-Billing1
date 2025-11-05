# Smart Sales Billing

Smart Sales Billing is a Spring Boot web application that captures retail invoice details, calculates GST automatically, renders a PDF bill, and optionally notifies customers over WhatsApp while triggering desktop printing. The UI is designed for quick data entry on iPhone 13, modern Android phones, and desktop browsers.

---

## 1. Project architecture and request lifecycle

The application follows a classic MVC layering with explicit services for PDF, messaging, and printing.

1. **HTTP request** – A user visits `/invoice/new` and submits the invoice form rendered by Thymeleaf templates (`src/main/resources/templates/invoice/form.html`).
2. **Controller layer** – `InvoiceController` receives the request, validates the bound `InvoiceForm`, and delegates to `InvoiceService`.
3. **Service layer** – `InvoiceService` performs GST calculations, persists the `Invoice` aggregate using `InvoiceRepository`, and orchestrates post-processing:
   - generates a PDF via `PdfService` using the `pdf.html` Thymeleaf template;
   - stores the PDF on disk and sends optional WhatsApp notifications through `WhatsAppService`;
   - asks `PrintService` to submit the PDF to the default printer when available.
4. **Persistence layer** – Spring Data JPA maps `Invoice` and `InvoiceItem` entities to the H2 database. Bill numbers auto-increment by re-saving with a `SS-00001` pattern once the database identifier is known.
5. **HTTP response** – The controller returns the success view (`success.html`), showing calculated totals and links to the generated PDF.

### Component diagram (logical)

```
Browser ↔ InvoiceController ↔ InvoiceService ↔ InvoiceRepository ↔ H2 (in-memory)
                                     ↘ PdfService → Thymeleaf pdf.html → OpenHTMLToPDF
                                      ↘ WhatsAppService → Twilio WhatsApp API (optional)
                                      ↘ PrintService → Java Desktop print (optional)
```

---

## 2. Source layout and file responsibilities

```
├── pom.xml                          # Maven build descriptor and dependency management
├── src
│   └── main
│       ├── java/com/example/billing
│       │   ├── BillingApplication.java     # Spring Boot bootstrap class
│       │   ├── catalog/HsnCodeCatalog.java # Five-item HSN code → description catalogue
│       │   ├── invoice/
│       │   │   ├── Invoice.java            # JPA entity for invoice header & totals
│       │   │   ├── InvoiceItem.java        # JPA entity for individual line items
│       │   │   └── InvoiceRepository.java  # Spring Data repository for CRUD access
│       │   ├── service/
│       │   │   ├── InvoiceService.java     # GST maths, persistence, PDF/print/WhatsApp orchestration
│       │   │   ├── PdfService.java         # Renders HTML templates into PDF bytes via OpenHTMLToPDF
│       │   │   ├── PrintService.java       # Sends PDFs to the OS default printer if supported
│       │   │   └── WhatsAppService.java    # Wraps Twilio client for WhatsApp notifications
│       │   └── web/
│       │       ├── InvoiceController.java  # HTTP endpoints for new invoice and success page
│       │       ├── InvoiceForm.java        # Form backing bean with validation annotations
│       │       └── InvoiceItemForm.java    # Nested form bean for each invoice item
│       └── resources
│           ├── application.properties      # Feature flags, storage, and Twilio credential keys
│           ├── static/
│           │   ├── css/invoice.css         # Mobile-first responsive layout styles
│           │   └── js/invoice.js           # Dynamic line-item handling and GST previews in-browser
│           └── templates/invoice/
│               ├── form.html               # Data-entry UI (auto-add rows, HSN dropdown)
│               ├── pdf.html                # Print-friendly template used during PDF rendering
│               └── success.html            # Confirmation page with summary details
└── README.md
```

---

## 3. Libraries and why they are used

| Dependency | Purpose |
|------------|---------|
| `spring-boot-starter-web` | Exposes REST endpoints, embedded Tomcat server |
| `spring-boot-starter-thymeleaf` | Renders server-side HTML templates for form, success, and PDF views |
| `spring-boot-starter-data-jpa` | Simplifies persistence with JPA/Hibernate |
| `spring-boot-starter-validation` | Validates form submissions with Jakarta Bean Validation |
| `com.h2database:h2` | In-memory relational DB for development/testing |
| `com.openhtmltopdf:openhtmltopdf-pdfbox` & `openhtmltopdf-slf4j` | Converts Thymeleaf-rendered HTML into PDF bytes |
| `org.projectlombok:lombok` (optional) | Not currently used in code, but available for future convenience |
| `spring-boot-starter-test` | Test harness (JUnit, AssertJ, Mockito) |

---

## 4. End-to-end workflow

1. User opens the invoice form on their phone or desktop.
2. Client-side JavaScript (`invoice.js`) helps add/remove line items, pre-fills descriptions from `HsnCodeCatalog`, and calculates GST previews.
3. On submit, Spring validates `InvoiceForm` and each `InvoiceItemForm`. Invalid submissions re-render `form.html` with error messages.
4. A valid form hits `InvoiceService#createInvoice`, which:
   - calculates net/base amounts, SGST, CGST, and totals using `BigDecimal` arithmetic;
   - persists the invoice graph via `InvoiceRepository` and assigns a sequential bill number;
   - renders `pdf.html` with invoice data to produce a PDF byte array via `PdfService`;
   - saves the PDF to the configured storage directory (default `src/main/resources/files`);
   - optionally sends a WhatsApp message with invoice details and a link (if `APP_WHATSAPP_ENABLED=true` and credentials exist);
   - optionally submits the PDF to the OS printer using `PrintService` (skips automatically if Java Desktop is unsupported).
5. The browser receives `success.html` with calculated totals, download link, and call-to-action for the next invoice.

---

## 5. Developer prerequisites and setup

- **Java Development Kit:** version 17 or later on the PATH.
- **Maven:** version 3.8 or later.
- **(Optional) Twilio sandbox:** to exercise WhatsApp notifications.
- **(Optional) Default printer:** to test printing from the JVM.

### Local run

```bash
mvn spring-boot:run
```

Access the application at <http://localhost:8080>. For mobile testing on the same network, expose the host machine IP and ensure firewalls permit port 8080.

### Packaging

```bash
mvn clean package
```

The runnable JAR will be emitted at `target/billing-0.0.1-SNAPSHOT.jar`.

> **Note:** In air-gapped environments Maven may fail to download the Spring Boot parent POM; mirror or pre-download dependencies as needed.

### Useful developer tools

- **H2 console:** `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:mem:billing` to inspect persisted invoices.
- **Thymeleaf caching disabled:** update HTML templates and refresh to see changes immediately during development.
- **Logging:** adjust `logging.level.com.example.billing=DEBUG` to trace service flow.

---

## 6. Deployment guidance

1. **Choose runtime** – Deploy the JAR on a Java 17 compatible environment (e.g., AWS Elastic Beanstalk, Azure App Service, on-prem Linux service, or Docker container).
2. **Externalise configuration** – Provide environment variables or properties files for:
   - `SPRING_DATASOURCE_*` if migrating from H2 to a managed database (recommended for production);
   - `APP_BASE_URL`, `APP_PDF-STORAGE`, and WhatsApp credentials.
3. **Database migration** – Replace H2 with PostgreSQL/MySQL, run schema DDL via JPA auto-generation or Flyway.
4. **Static assets** – Served from the packaged JAR; behind a reverse proxy enable gzip and caching headers for faster loads on mobile.
5. **Security** – Restrict access to authenticated staff (e.g., add Spring Security) and secure WhatsApp webhooks over HTTPS.
6. **Scaling** – For multiple instances, use an external file store (S3, Azure Blob, NFS) for PDFs and a shared database to avoid bill number collisions.

### Production checklist

- [ ] Configure HTTPS termination.
- [ ] Point `APP_BASE_URL` to the public hostname (used in WhatsApp message links).
- [ ] Ensure the default printer service is available or disable printing via configuration if running headless.
- [ ] Set up monitoring (e.g., CloudWatch, ELK) for `com.example.billing` logs.
- [ ] Schedule backups of the persistent database and stored PDFs.

---

## 7. Debugging and production issue resolution

1. **Reproduce locally** – Pull the production invoice entry (via database query) and replay the payload using the H2 console or a REST client to isolate logic errors.
2. **Inspect logs** – Enable DEBUG logs for `InvoiceService`, `PdfService`, and `WhatsAppService` to trace the pipeline.
3. **Check generated files** – Confirm PDFs exist in the configured directory and have read permissions.
4. **WhatsApp issues** – Verify Twilio credentials, sender/receiver numbers, and ensure `APP_BASE_URL` resolves publicly. Review Twilio console delivery logs.
5. **Printing failures** – Confirm the JVM runs in a desktop environment. `PrintService` logs when printing is skipped or an error occurs.
6. **Database integrity** – For missing bill numbers, ensure the database sequence is intact; resetting the table or switching to a robust RDBMS may be necessary.

---

## 8. API regression testing with Postman

Automated smoke tests for the three primary endpoints ship with the repository under `postman/`:

- `postman/SmartSalesBilling.postman_collection.json` – Covers the form render (`GET /`), invoice submission (`POST /invoices`), and PDF download (`GET /invoices/{id}/pdf`). Pre-request scripts seed required form fields and chain the generated invoice identifier, while test scripts assert HTML/PDF responses and persist the bill number.
- `postman/SmartSalesBilling.postman_environment.json` – Provides the `baseUrl` (default `http://localhost:8080`) plus mutable `invoiceId`/`billNumber` variables captured during the run.

To execute:

1. Import the collection and environment into Postman (or Newman).
2. Start the Spring Boot application locally (`mvn spring-boot:run`).
3. Select the **Smart Sales Billing Local** environment.
4. Run the collection. All tests should pass, confirming the end-to-end flow from HTML form submission through PDF generation.

When issues persist, capture thread dumps (`jcmd <pid> Thread.print`) and heap stats (`jcmd <pid> GC.heap_info`) to provide additional diagnostics.

---

## 8. Customer requirements for end-to-end usage

To operate Smart Sales Billing in a retail setting, the business must provide:

- **Device access:** Smartphone (iPhone 13/Android) or desktop/laptop with a modern browser.
- **Network connectivity:** Local Wi-Fi or mobile data to reach the hosted application.
- **Business metadata:** Customer name, address, phone, GSTIN/UID, delivery dates, vehicle number, place of supply, and per-item final prices.
- **HSN mapping:** Optional overrides for descriptions, otherwise use the five preloaded HSN codes.
- **Printing setup:** A printer connected to the server/desktop running the application if physical invoices are required.
- **WhatsApp integration (optional):** Twilio WhatsApp account, verified sending number, customer opt-ins, and a publicly reachable app URL.
- **Signature/stamp:** Space on the printed PDF is provided; supply your own signature image or sign manually after printing.

With these inputs the user completes the form, reviews the calculated GST totals, generates the PDF, and optionally shares it over WhatsApp or prints it for the customer.

---

## 9. Frequently referenced code paths

- **GST calculation:** `InvoiceService#createInvoice`.
- **HSN catalogue source:** `HsnCodeCatalog`.
- **UI scripts and styling:** `static/js/invoice.js`, `static/css/invoice.css`.
- **PDF HTML template:** `templates/invoice/pdf.html`.
- **WhatsApp payload:** `WhatsAppService#sendInvoice`.

Refer to these files when extending or debugging the application.

---

## 10. Licensing

Distributed under the MIT License. See [LICENSE](LICENSE) for full text.

