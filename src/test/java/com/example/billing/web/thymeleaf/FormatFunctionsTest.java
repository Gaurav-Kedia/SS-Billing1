package com.example.billing.web.thymeleaf;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FormatFunctionsTest {

    private FormatFunctions formatFunctions;

    @BeforeEach
    void setUp() {
        formatFunctions = new FormatFunctions();
    }

    @Test
    void formatsAmountUsingIndianGrouping() {
        String value = formatFunctions.amount(new BigDecimal("1234567.89"));
        assertThat(value).isEqualTo("12,34,567.89");
    }

    @Test
    void prefixesRupeeSymbolForMoney() {
        String value = formatFunctions.money(new BigDecimal("9876543.21"));
        assertThat(value).isEqualTo("₹ 98,76,543.21");
    }

    @Test
    void keepsMinusSignAfterSymbolForNegativeMoney() {
        String value = formatFunctions.money(new BigDecimal("-987.65"));
        assertThat(value).isEqualTo("₹ -987.65");
    }

    @Test
    void formatsRoundedMoneyWithoutDecimals() {
        String value = formatFunctions.roundedMoney(new BigDecimal("1234.49"));
        assertThat(value).isEqualTo("₹ 1,234");
    }

    @Test
    void calculatesRoundOffDifferenceWithSymbol() {
        String value = formatFunctions.roundOff(new BigDecimal("1234.49"));
        assertThat(value).isEqualTo("₹ -0.49");
    }

    @Test
    void formatsPerUnitAmount() {
        String value = formatFunctions.perUnit(new BigDecimal("100.00"), 4);
        assertThat(value).isEqualTo("25.00");
    }

    @Test
    void convertsAmountInWordsWithRupeesPrefix() {
        String value = formatFunctions.amountInWords(new BigDecimal("1234567.89"));
        assertThat(value).isEqualTo("RUPEES TWELVE LAKH THIRTY FOUR THOUSAND FIVE HUNDRED SIXTY SEVEN AND EIGHTY NINE PAISE ONLY");
    }

    @Test
    void convertsNegativeAmountInWords() {
        String value = formatFunctions.amountInWords(new BigDecimal("-101.05"));
        assertThat(value).isEqualTo("MINUS RUPEES ONE HUNDRED ONE AND FIVE PAISE ONLY");
    }

    @Test
    void convertsZeroAmountInWords() {
        String value = formatFunctions.amountInWords(BigDecimal.ZERO);
        assertThat(value).isEqualTo("RUPEES ZERO ONLY");
    }
}
