package com.example.shoes_store;

import com.example.shoes_store.module.admin.AdminService;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.payment.PaymentModel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AdminBusinessTest {

    @Test
    public void isAdminRoleRecognizesCaseInsensitiveAdminAndNormalizesUsers() {
        assertTrue(AdminService.isAdminRole("admin"));
        assertTrue(AdminService.isAdminRole("ADMIN"));
        assertTrue(AdminService.isAdminRole(" admin "));
        assertFalse(AdminService.isAdminRole("users"));
        assertFalse(AdminService.isAdminRole("CUSTOMER"));
        assertFalse(AdminService.isAdminRole(null));

        assertEquals("admin", AdminService.normalizeRole("admin"));
        assertEquals("admin", AdminService.normalizeRole("ADMIN"));
        assertEquals("users", AdminService.normalizeRole("users"));
        assertEquals("users", AdminService.normalizeRole(null));

        assertTrue(AdminService.isDefaultAdminEmail("admin@gmail.com"));
        assertEquals("admin", AdminService.resolveRole("users", "admin@gmail.com"));
        assertEquals("users", AdminService.resolveRole("users", "bao123@gmail.com"));
    }

    @Test
    public void orderStateMachineAllowsOnlyValidAdminTransitions() {
        assertTrue(AdminService.canAdvanceOrderStatus(OrderModel.Status.PENDING, OrderModel.Status.SHIPPING));
        assertTrue(AdminService.canAdvanceOrderStatus(OrderModel.Status.PENDING, OrderModel.Status.CANCELLED));
        assertFalse(AdminService.canAdvanceOrderStatus(OrderModel.Status.PENDING, OrderModel.Status.COMPLETED));

        assertTrue(AdminService.canAdvanceOrderStatus(OrderModel.Status.SHIPPING, OrderModel.Status.COMPLETED));
        assertFalse(AdminService.canAdvanceOrderStatus(OrderModel.Status.SHIPPING, OrderModel.Status.CANCELLED));

        assertFalse(AdminService.canAdvanceOrderStatus(OrderModel.Status.COMPLETED, OrderModel.Status.CANCELLED));
        assertFalse(AdminService.canAdvanceOrderStatus(OrderModel.Status.CANCELLED, OrderModel.Status.SHIPPING));
    }

    @Test
    public void kpiCountersAndCompletedRevenueAreAccurate() {
        List<OrderModel> orders = Arrays.asList(
                buildOrder("o1", OrderModel.Status.PENDING, 3_500_000L),
                buildOrder("o2", OrderModel.Status.SHIPPING, 4_200_000L),
                buildOrder("o3", OrderModel.Status.COMPLETED, 3_500_000L),
                buildOrder("o4", OrderModel.Status.COMPLETED, 7_000_000L),
                buildOrder("o5", OrderModel.Status.CANCELLED, 5_000_000L)
        );
        assertEquals(1, AdminService.countOrdersByStatus(orders, OrderModel.Status.PENDING));
        assertEquals(1, AdminService.countOrdersByStatus(orders, OrderModel.Status.SHIPPING));
        assertEquals(2, AdminService.countOrdersByStatus(orders, OrderModel.Status.COMPLETED));
        assertEquals(10_500_000L, AdminService.calculateCompletedRevenue(orders));
    }

    @Test
    public void validateProductAndVoucherFormsCatchInvalidInputs() {
        assertNotNull(AdminService.validateProductForm("", "Nike", "Daily", 3_000_000L, 0L, 10));
        assertNotNull(AdminService.validateProductForm("Pegasus", "Nike", "Daily", 0L, 0L, 10));
        assertNotNull(AdminService.validateProductForm("Pegasus", "Nike", "Daily", 3_500_000L, 3_000_000L, 10));
        assertNotNull(AdminService.validateProductForm("Pegasus", "Nike", "Daily", 3_000_000L, 3_500_000L, -1));
        assertNull(AdminService.validateProductForm("Pegasus 41", "Nike", "Daily", 3_290_000L, 3_690_000L, 8));

        assertNotNull(AdminService.validateVoucherForm("AB", "Giảm 100K", 100_000L, 0L, 10));
        assertNotNull(AdminService.validateVoucherForm("RUN100K", "", 100_000L, 0L, 10));
        assertNotNull(AdminService.validateVoucherForm("RUN100K", "Giảm 100K", 0L, 0L, 10));
        assertNull(AdminService.validateVoucherForm("RUN100K", "Giảm 100K", 100_000L, 1_000_000L, 50));

        assertEquals(Arrays.asList("40", "41", "42"), AdminService.parseCsvList("40, 41, 42, 40", Collections.singletonList("42")));
    }

    private OrderModel buildOrder(String id, OrderModel.Status status, long total) {
        return new OrderModel(
                id, id, "u1", status, PaymentModel.Method.COD,
                Collections.emptyList(), "Q1", "Bảo", "Giao chuẩn", "",
                total, 0L, 0L, total, 1000L, 1000L
        );
    }
}
