package com.example.billing.web.thymeleaf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("formatting")
public class FormatFunctions {

    private static final String RUPEE_SYMBOL = "₹";

    private static final ThreadLocal<DecimalFormat> INDIAN_MONEY_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("en", "IN"));
        DecimalFormat decimalFormat = new DecimalFormat("#,##,##0.00", symbols);
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        return decimalFormat;
    });

    private static final ThreadLocal<DecimalFormat> INDIAN_INTEGER_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("en", "IN"));
        DecimalFormat decimalFormat = new DecimalFormat("#,##,##0", symbols);
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
        return decimalFormat;
    });

    private static String format(ThreadLocal<DecimalFormat> holder, BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return holder.get().format(amount);
    }

    public String amount(BigDecimal amount) {
        return format(INDIAN_MONEY_FORMAT, amount);
    }

    public String money(BigDecimal amount) {
        String numeric = amount(amount);
        if (numeric.isEmpty()) {
            return "";
        }
        return RUPEE_SYMBOL + " " + numeric;
    }

    public String roundedMoney(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        return RUPEE_SYMBOL + " " + INDIAN_INTEGER_FORMAT.get().format(rounded);
    }

    public String roundOff(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        BigDecimal difference = rounded.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        return RUPEE_SYMBOL + " " + INDIAN_MONEY_FORMAT.get().format(difference);
    }

    public String perUnit(BigDecimal totalAmount, Integer quantity) {
        if (totalAmount == null || quantity == null || quantity <= 0) {
            return "";
        }
        BigDecimal perUnit = totalAmount.divide(new BigDecimal(quantity), 2, RoundingMode.HALF_UP);
        return amount(perUnit);
    }
}
