package com.example.shoes_store;

import com.example.shoes_store.module.admin.AdminService;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.payment.PaymentModel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AdminBusinessTest {

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
        assertEquals(10_500_000L, AdminService.calculateCompletedRevenue(orders));
        assertEquals(0L, AdminService.calculateCompletedRevenue(null));
        assertEquals(0L, AdminService.calculateCompletedRevenue(Collections.emptyList()));
    }

    @Test
    public void validateProductAndVoucherFormsCatchInvalidInputs() {
        assertNotNull(AdminService.validateProductForm("", "Nike", "Daily", 3_000_000L, 0L, 10));
        assertNotNull(AdminService.validateProductForm("Pegasus", "Nike", "Daily", 0L, 0L, 10));
        assertNotNull(AdminService.validateProductForm("Pegasus", "Nike", "Daily", 3_500_000L, 3_000_000L, 10));
        assertNotNull(AdminService.validateProductForm("Pegasus", "Nike", "Daily", 3_000_000L, 3_500_000L, -1));
        assertNull(AdminService.validateProductForm("Pegasus 41", "Nike", "Daily", 3_290_000L, 3_690_000L, 8));
        assertNull(AdminService.validateProductForm("Pegasus 41", "Nike", "Daily", 3_290_000L, 3_690_000L, (Integer) null));
        assertNotNull(AdminService.validateProductForm("Pegasus 41", "Nike", "Daily", 3_290_000L, 3_690_000L, Integer.valueOf(-1)));

        Map<String, Integer> maxStock = new HashMap<>();
        maxStock.put("39", 5);
        maxStock.put("40", 0);
        maxStock.put("41", 3);
        assertEquals(
                Collections.singletonList("40"),
                AdminService.computeOutOfStockSizes(Arrays.asList("39", "40", "41"), maxStock)
        );

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
                total, 0L, 0L, total, 1000L
        );
    }
}
