package com.finance.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyFormatterTest {

    @Test
    void formatsWithTwoDecimalsAndThousandsSeparator() {
        MoneyFormatter formatter = new MoneyFormatter("€");
        assertEquals("€0.00", formatter.format(BigDecimal.ZERO));
        assertEquals("€12.50", formatter.format(new BigDecimal("12.5")));
        assertEquals("€1,234,567.89", formatter.format(new BigDecimal("1234567.89")));
    }

    @Test
    void negativeAmountsGetLeadingMinus() {
        assertEquals("-€20.00", new MoneyFormatter("€").format(new BigDecimal("-20")));
    }

    @Test
    void multiCharacterSymbolIsSeparatedBySpace() {
        assertEquals("EUR 5.00", new MoneyFormatter("EUR").format(new BigDecimal("5")));
    }
}
