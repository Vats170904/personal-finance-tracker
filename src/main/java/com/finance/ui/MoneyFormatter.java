package com.finance.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

/**
 * Formats amounts for display, e.g. {@code €1,234.50} or {@code -€20.00}.
 *
 * <p>The currency symbol is only added here, never in calculations, so it can be
 * changed (for example to "EUR" on terminals that cannot show the euro sign).
 */
public final class MoneyFormatter {

    private final String currencyPrefix;
    private final DecimalFormat numberFormat;

    public MoneyFormatter(String currencySymbol) {
        Objects.requireNonNull(currencySymbol, "currencySymbol");
        // A single-character symbol is written directly before the number (€5.00);
        // a code like "EUR" gets a space (EUR 5.00).
        this.currencyPrefix = currencySymbol.length() == 1 ? currencySymbol : currencySymbol + " ";
        this.numberFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        this.numberFormat.setRoundingMode(RoundingMode.HALF_EVEN);
    }

    public String format(BigDecimal amount) {
        String sign = amount.signum() < 0 ? "-" : "";
        return sign + currencyPrefix + numberFormat.format(amount.abs());
    }
}
