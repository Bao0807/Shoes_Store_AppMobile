package com.example.shoes_store.core;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Định dạng tiền tệ VND theo chuẩn Việt Nam (ví dụ: 3.490.000₫).
 */
public final class CurrencyFormatUtil {

    private static final ThreadLocal<DecimalFormat> VND_FORMAT = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,###", symbols);
    });

    private CurrencyFormatUtil() {
    }

    public static String formatVnd(long amount) {
        DecimalFormat formatter = VND_FORMAT.get();
        return formatter != null ? formatter.format(amount) + "₫" : amount + "₫";
    }
}
