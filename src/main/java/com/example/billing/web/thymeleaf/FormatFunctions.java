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
    private static final Locale UPPERCASE_LOCALE = Locale.ENGLISH;

    private static final String[] BELOW_TWENTY = { "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen" };
    private static final String[] TENS = { "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety" };

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

    public String amountInWords(BigDecimal amount) {
        if (amount == null) {
            return "";
        }

        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = normalized.setScale(0, RoundingMode.DOWN).longValue();
        int paise = normalized.subtract(new BigDecimal(rupees)).movePointRight(2).intValue();

        StringBuilder words = new StringBuilder();
        words.append("Rupees ");
        words.append(numberToWords(rupees));

        if (paise > 0) {
            words.append(" and ");
            words.append(numberToWords(paise));
            words.append(" Paise");
        }

        words.append(" Only");
        return words.toString().toUpperCase(UPPERCASE_LOCALE);
    }

    private static String numberToWords(long number) {
        if (number == 0) {
            return BELOW_TWENTY[0];
        }

        StringBuilder result = new StringBuilder();

        int crore = (int) (number / 10000000);
        number %= 10000000;
        int lakh = (int) (number / 100000);
        number %= 100000;
        int thousand = (int) (number / 1000);
        number %= 1000;
        int remainder = (int) number;

        appendSection(result, crore, "Crore");
        appendSection(result, lakh, "Lakh");
        appendSection(result, thousand, "Thousand");

        if (remainder > 0) {
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(convertBelowThousand(remainder));
        }

        return result.toString().trim();
    }

    private static void appendSection(StringBuilder builder, int number, String label) {
        if (number <= 0) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(convertBelowThousand(number)).append(' ').append(label);
    }

    private static String convertBelowThousand(int number) {
        StringBuilder builder = new StringBuilder();
        int hundred = number / 100;
        int remainder = number % 100;

        if (hundred > 0) {
            builder.append(BELOW_TWENTY[hundred]).append(" Hundred");
            if (remainder > 0) {
                builder.append(' ');
            }
        }

        if (remainder > 0) {
            builder.append(convertBelowHundred(remainder));
        }

        return builder.toString();
    }

    private static String convertBelowHundred(int number) {
        if (number < 20) {
            return BELOW_TWENTY[number];
        }
        int tensPlace = number / 10;
        int onesPlace = number % 10;
        if (onesPlace == 0) {
            return TENS[tensPlace];
        }
        return TENS[tensPlace] + ' ' + BELOW_TWENTY[onesPlace];
    }
}
