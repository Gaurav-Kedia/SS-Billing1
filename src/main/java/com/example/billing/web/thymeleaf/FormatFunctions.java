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

    private static final String[] BELOW_TWENTY = {
            "Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static final String[] GROUP_UNITS = {
            "", "Thousand", "Lakh", "Crore", "Arab", "Kharab", "Neel", "Padma", "Shankh"
    };

    private static final int[] GROUP_SIZES = {3, 2, 2, 2, 2, 2, 2, 2, 2};

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
        DecimalFormat decimalFormat = holder.get();
        synchronized (decimalFormat) {
            return decimalFormat.format(amount);
        }
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
        boolean negative = normalized.signum() < 0;
        normalized = normalized.abs();
        if (normalized.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            return "";
        }
        long rupees = normalized.longValue();
        int paise = normalized.subtract(BigDecimal.valueOf(rupees)).movePointRight(2).intValue();

        StringBuilder words = new StringBuilder();
        if (negative) {
            words.append("Minus ");
        }

        if (rupees == 0) {
            words.append("Zero Rupees");
        } else {
            words.append(convertNumberToWords(rupees)).append(rupees == 1 ? " Rupee" : " Rupees");
        }

        if (paise > 0) {
            words.append(" and ").append(convertNumberToWords(paise))
                    .append(paise == 1 ? " Paisa" : " Paise");
        }

        words.append(" Only");
        return words.toString().toUpperCase(Locale.ENGLISH);
    }

    private static String convertNumberToWords(long number) {
        if (number == 0) {
            return BELOW_TWENTY[0];
        }
        StringBuilder result = new StringBuilder();
        int unitIndex = 0;
        long remaining = number;
        while (remaining > 0 && unitIndex < GROUP_UNITS.length) {
            int groupSize = GROUP_SIZES[unitIndex];
            long divisor = powerOfTen(groupSize);
            int groupValue = (int) (remaining % divisor);
            if (groupValue > 0) {
                String segment = convertBelowThousand(groupValue);
                if (!GROUP_UNITS[unitIndex].isEmpty()) {
                    segment = segment + " " + GROUP_UNITS[unitIndex];
                }
                if (result.length() > 0) {
                    segment = segment + " ";
                }
                result.insert(0, segment);
            }
            remaining /= divisor;
            unitIndex++;
        }
        return result.toString().trim();
    }

    private static String convertBelowThousand(int number) {
        StringBuilder builder = new StringBuilder();
        int hundreds = number / 100;
        int remainder = number % 100;
        if (hundreds > 0) {
            builder.append(BELOW_TWENTY[hundreds]).append(" Hundred");
            if (remainder > 0) {
                builder.append(" ");
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
        int tens = number / 10;
        int units = number % 10;
        if (units == 0) {
            return TENS[tens];
        }
        return TENS[tens] + " " + BELOW_TWENTY[units];
    }

    private static long powerOfTen(int exponent) {
        long result = 1L;
        for (int i = 0; i < exponent; i++) {
            result *= 10L;
        }
        return result;
    }
}
