package com.example.shoes_store;

import static org.junit.Assert.assertEquals;

import com.example.shoes_store.core.DateFormatUtil;
import com.example.shoes_store.module.order.OrderService;

import org.junit.Test;

import java.util.TimeZone;

/**
 * Kiểm thử đơn vị cho DateFormatUtil đảm bảo định dạng giống hệt trước khi gom về core.
 */
public class DateFormatUtilTest {

    // 2025-03-10T02:30:45Z
    private static final long FIXED_MILLIS = 1_741_573_845_000L;

    @Test
    public void formatAllPatternsInHoChiMinhTimeZone() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            assertEquals("10/03/2025", DateFormatUtil.formatDate(FIXED_MILLIS));
            assertEquals("10/03/2025 · 09:30", DateFormatUtil.formatDateTime(FIXED_MILLIS));
            assertEquals("250310093045", DateFormatUtil.formatOrderCodeTimestamp(FIXED_MILLIS));
            assertEquals("RN250310093045", OrderService.generateOrderCode(FIXED_MILLIS));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    public void formatRespectsDefaultTimeZoneChange() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertEquals("10/03/2025 · 02:30", DateFormatUtil.formatDateTime(FIXED_MILLIS));
            assertEquals("250310023045", DateFormatUtil.formatOrderCodeTimestamp(FIXED_MILLIS));
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
