# Smart Sales Billing

Smart Sales Billing is a lightweight Spring Boot web application for creating GST compliant invoices for Shankar Sales. The app provides a mobile-friendly form, automatically calculates taxes, generates polished PDFs, sends optional WhatsApp notifications, and can trigger local printing.

## Features

- Responsive invoice form optimised for iPhone, Android, and desktop browsers
- Auto-incrementing bill numbers with persistence
- Customer capture: name, address, phone, GSTIN/UID
- Fixed state information (ODISHA, code 21)
- Delivery metadata: issue date, delivery date, vehicle number, place of supply
- Itemised billing with pre-populated HSN codes and editable descriptions
- Automatic GST split (9% SGST + 9% CGST) with base value calculations
- Discount support, real-time totals, and signature placeholders
- PDF generation using OpenHTMLtoPDF with a layout matching the provided sample
- Optional WhatsApp notification using the Twilio API (with hosted PDF link)
- Optional desktop printing using the Java Desktop integration

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+

### Running the application

```bash
mvn spring-boot:run
```

The application starts on [http://localhost:8080](http://localhost:8080). Navigate to this URL from your browser or mobile device on the same network.

### Building

```bash
mvn clean package
```

The compiled JAR is placed under `target/billing-0.0.1-SNAPSHOT.jar`.

## WhatsApp integration

The application can notify customers via WhatsApp after an invoice is generated. It uses Twilio's WhatsApp API. To enable it:

1. Provision a Twilio account with WhatsApp sandbox or production access.
2. Expose the application over the internet (for example using [ngrok](https://ngrok.com/)) so Twilio can fetch the generated PDF.
3. Configure the following properties in `src/main/resources/application.properties` or via environment variables:
   - `APP_WHATSAPP_ENABLED=true`
   - `APP_WHATSAPP_ACCOUNT_SID=<twilio-account-sid>`
   - `APP_WHATSAPP_AUTH_TOKEN=<twilio-auth-token>`
   - `APP_WHATSAPP_FROM_NUMBER=whatsapp:+14155238886` (replace with your sender)
   - `APP_BASE_URL=https://your-hostname`
4. Restart the application.

If credentials are missing the service logs a warning and skips sending the message.

## PDF storage and printing

Generated PDFs are stored in the `generated-pdfs` directory relative to the app working directory. Desktop printing leverages the Java Desktop API and will only work on systems with a default printer configured. In headless environments the service logs a warning and continues.

## Data persistence

Invoices are stored in an in-memory H2 database for simplicity. To use a persistent database, update the Spring datasource settings in `application.properties`.

## Development tips

- Access the H2 console at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:billing`).
- Thymeleaf caching is disabled for rapid template iteration.
- The default PDF template uses standard fonts; install `DejaVu Sans` on the host if custom characters are required.

## License

This project is provided as-is under the MIT License. See [LICENSE](LICENSE) for details.
