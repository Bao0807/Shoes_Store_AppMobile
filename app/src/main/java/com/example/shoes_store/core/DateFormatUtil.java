package com.example.shoes_store.core;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tiện ích định dạng ngày giờ thống nhất toàn ứng dụng (an toàn đa luồng, giữ múi giờ mặc định).
 */
public final class DateFormatUtil {

    private static final String PATTERN_DATE = "dd/MM/yyyy";
    private static final String PATTERN_DATE_TIME = "dd/MM/yyyy · HH:mm";
    private static final String PATTERN_ORDER_CODE = "yyMMddHHmmss";

    private DateFormatUtil() {
    }

    @NonNull
    public static String formatDate(long millis) {
        return new SimpleDateFormat(PATTERN_DATE, Locale.US).format(new Date(millis));
    }

    @NonNull
    public static String formatDateTime(long millis) {
        return new SimpleDateFormat(PATTERN_DATE_TIME, Locale.US).format(new Date(millis));
    }

    @NonNull
    public static String formatOrderCodeTimestamp(long millis) {
        return new SimpleDateFormat(PATTERN_ORDER_CODE, Locale.US).format(new Date(millis));
    }
}
