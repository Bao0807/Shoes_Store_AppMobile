package com.example.shoes_store.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tiện ích đọc an toàn các kiểu dữ liệu nguyên thủy từ Firestore DocumentSnapshot và Map.
 */
public final class FirestoreReadUtil {

    private FirestoreReadUtil() {
    }

    @NonNull
    public static String readString(@NonNull DocumentSnapshot doc, @NonNull String key, @NonNull String fallback) {
        String val = doc.getString(key);
        return val != null ? val : fallback;
    }

    public static long readLong(@NonNull DocumentSnapshot doc, @NonNull String key, long fallback) {
        Long val = doc.getLong(key);
        return val != null ? val : fallback;
    }

    public static int readInt(@NonNull DocumentSnapshot doc, @NonNull String key, int fallback) {
        Long val = doc.getLong(key);
        return val != null ? val.intValue() : fallback;
    }

    public static double readDouble(@NonNull DocumentSnapshot doc, @NonNull String key, double fallback) {
        Double val = doc.getDouble(key);
        return val != null ? val : fallback;
    }

    public static boolean readBoolean(@NonNull DocumentSnapshot doc, @NonNull String key, boolean fallback) {
        Boolean val = doc.getBoolean(key);
        return val != null ? val : fallback;
    }

    public static long readMillis(@NonNull DocumentSnapshot doc, @NonNull String key) {
        Timestamp ts = doc.getTimestamp(key);
        if (ts != null) {
            return ts.toDate().getTime();
        }
        Long num = doc.getLong(key);
        return num != null ? num : 0L;
    }

    @NonNull
    public static List<String> readStringList(@NonNull DocumentSnapshot doc, @NonNull String key) {
        List<String> result = new ArrayList<>();
        Object raw = doc.get(key);
        if (raw instanceof List<?>) {
            for (Object item : (List<?>) raw) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
        }
        return result;
    }

    @NonNull
    public static String mapString(@Nullable Map<?, ?> map, @NonNull String key, @NonNull String fallback) {
        if (map == null) {
            return fallback;
        }
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : fallback;
    }

    public static long mapLong(@Nullable Map<?, ?> map, @NonNull String key, long fallback) {
        if (map == null) {
            return fallback;
        }
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        }
        return fallback;
    }

    public static int mapInt(@Nullable Map<?, ?> map, @NonNull String key, int fallback) {
        if (map == null) {
            return fallback;
        }
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return fallback;
    }
}
