package com.example.billing.web.thymeleaf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("formatting")
public class FormatFunctions {

    private static final ThreadLocal<DecimalFormat> INDIAN_MONEY_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("en", "IN"));
        DecimalFormat decimalFormat = new DecimalFormat("#,##,##0.00", symbols);
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        return decimalFormat;
    });

    public String money(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return INDIAN_MONEY_FORMAT.get().format(amount);
    }
}
