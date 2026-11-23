package com.example.shoes_store.module.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.payment.PaymentModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Nghiệp vụ thuần Java cho Kênh Quản trị (Admin): phân quyền, máy trạng thái đơn hàng,
 * tính toán thống kê doanh thu và kiểm tra biểu mẫu Sản phẩm / Voucher.
 */
public final class AdminService {

    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USERS = "users";
    public static final String DEFAULT_ADMIN_EMAIL = "admin@gmail.com";
    public static final String DEFAULT_ADMIN_PASS = "123456";

    private AdminService() {
    }

    public static boolean isDefaultAdminEmail(@Nullable String email) {
        return email != null && DEFAULT_ADMIN_EMAIL.equalsIgnoreCase(email.trim());
    }

    public static boolean isAdminRole(@Nullable String role) {
        if (role == null) {
            return false;
        }
        String clean = role.trim();
        return ROLE_ADMIN.equalsIgnoreCase(clean);
    }

    @NonNull
    public static String normalizeRole(@Nullable String roleValue) {
        return isAdminRole(roleValue) ? ROLE_ADMIN : ROLE_USERS;
    }

    @NonNull
    public static String resolveRole(@Nullable String roleValue, @Nullable String email) {
        if (isDefaultAdminEmail(email) || isAdminRole(roleValue)) {
            return ROLE_ADMIN;
        }
        return ROLE_USERS;
    }

    public static boolean canAdvanceOrderStatus(
            @Nullable OrderModel.Status current,
            @Nullable OrderModel.Status next
    ) {
        if (current == null || next == null || current == next) {
            return false;
        }
        if (current == OrderModel.Status.PENDING) {
            return next == OrderModel.Status.SHIPPING || next == OrderModel.Status.CANCELLED;
        }
        if (current == OrderModel.Status.SHIPPING) {
            return next == OrderModel.Status.COMPLETED;
        }
        return false;
    }

    @NonNull
    public static PaymentModel.Status resolvePaymentStatusOnOrderComplete() {
        return PaymentModel.Status.PAID;
    }

    @NonNull
    public static String buildOrderStatusNotificationTitle(
            @NonNull String orderCode,
            @NonNull OrderModel.Status nextStatus
    ) {
        switch (nextStatus) {
            case SHIPPING:
                return "Đơn #" + orderCode + " đã được xác nhận & giao";
            case COMPLETED:
                return "Đơn #" + orderCode + " đã giao thành công";
            case CANCELLED:
            default:
                return "Đơn #" + orderCode + " đã bị hủy bởi cửa hàng";
        }
    }

    @NonNull
    public static String buildOrderStatusNotificationMessage(@NonNull OrderModel.Status nextStatus) {
        switch (nextStatus) {
            case SHIPPING:
                return "Cửa hàng đã xác nhận và bàn giao đơn giày của bạn cho đơn vị vận chuyển.";
            case COMPLETED:
                return "Cảm ơn bạn đã mua sắm! Bạn đã có thể viết đánh giá cho sản phẩm trong đơn.";
            case CANCELLED:
            default:
                return "Đơn hàng đã được hủy và hoàn lại số lượng kho / lượt ưu đãi.";
        }
    }

    public static int countOrdersByStatus(
            @Nullable List<OrderModel> orders,
            @NonNull OrderModel.Status status
    ) {
        if (orders == null || orders.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (OrderModel order : orders) {
            if (order != null && order.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    public static long calculateCompletedRevenue(@Nullable List<OrderModel> orders) {
        if (orders == null || orders.isEmpty()) {
            return 0L;
        }
        long revenue = 0L;
        for (OrderModel order : orders) {
            if (order != null && order.getStatus() == OrderModel.Status.COMPLETED) {
                revenue += Math.max(0L, order.getTotal());
            }
        }
        return revenue;
    }

    @Nullable
    public static String validateProductForm(
            @Nullable String name,
            @Nullable String brand,
            @Nullable String category,
            long price,
            long originalPrice,
            int stockPerVariant
    ) {
        if (name == null || name.trim().isEmpty()) {
            return "Vui lòng nhập tên mẫu giày";
        }
        if (brand == null || brand.trim().isEmpty()) {
            return "Vui lòng nhập thương hiệu (vd: Nike, ASICS, HOKA)";
        }
        if (category == null || category.trim().isEmpty()) {
            return "Vui lòng chọn dòng giày (Daily, Race, Tempo, Trail, Recovery)";
        }
        if (price <= 0L) {
            return "Giá bán phải lớn hơn 0₫";
        }
        if (originalPrice > 0L && originalPrice < price) {
            return "Giá gốc không được nhỏ hơn giá bán";
        }
        if (stockPerVariant < 0) {
            return "Số lượng tồn kho mỗi size không được âm";
        }
        return null;
    }

    @Nullable
    public static String validateVoucherForm(
            @Nullable String code,
            @Nullable String title,
            long discountAmount,
            long minOrderAmount,
            int usageLimit
    ) {
        if (code == null || code.trim().length() < 3) {
            return "Mã voucher phải có ít nhất 3 ký tự";
        }
        if (title == null || title.trim().isEmpty()) {
            return "Vui lòng nhập tiêu đề ưu đãi";
        }
        if (discountAmount <= 0L) {
            return "Số tiền giảm phải lớn hơn 0₫";
        }
        if (minOrderAmount < 0L) {
            return "Đơn tối thiểu không được âm";
        }
        if (usageLimit < 0) {
            return "Giới hạn lượt dùng không được âm";
        }
        return null;
    }

    @NonNull
    public static List<String> parseCsvList(@Nullable String raw, @NonNull List<String> fallback) {
        List<String> result = new ArrayList<>();
        if (raw != null && !raw.trim().isEmpty()) {
            String[] parts = raw.split(",");
            for (String p : parts) {
                String clean = p.trim();
                if (!clean.isEmpty() && !result.contains(clean)) {
                    result.add(clean);
                }
            }
        }
        return result.isEmpty() ? new ArrayList<>(fallback) : result;
    }
}
