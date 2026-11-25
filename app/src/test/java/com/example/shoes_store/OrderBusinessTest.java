package com.example.shoes_store;

import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.order.OrderService;
import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.product.ProductModel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OrderBusinessTest {

    @Test
    public void orderServiceCalculatesTotalsAndSubtotal() {
        ProductModel shoe = new ProductModel(
                "p1", "Novablast 5", "ASICS", "Daily", 3_000_000L, 3_500_000L,
                5.0, 2, 0, "", Collections.emptyList(), "255g", "8mm", "Cao", "Road",
                "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptySet(), true, 1000L
        );
        CartItemModel item1 = new CartItemModel("c1", shoe, "v1", "42", "Cam", 2, true, 1000L);
        CartItemModel item2 = new CartItemModel("c2", shoe, "v2", "43", "Đen", 1, true, 1000L);

        long subtotal = OrderService.calculateSubtotal(Arrays.asList(item1, item2));
        assertEquals(9_000_000L, subtotal);
        assertEquals(8_880_000L, OrderService.calculateTotal(subtotal, 30_000L, 150_000L));
        assertEquals(0L, OrderService.calculateTotal(100_000L, 0L, 500_000L));
        assertTrue(OrderService.generateOrderCode(1_700_000_000_000L).startsWith("RN"));
    }

    @Test
    public void orderServiceAggregatesAndValidatesStock() {
        ProductModel shoe = new ProductModel(
                "p1", "Novablast 5", "ASICS", "Daily", 3_000_000L, 3_000_000L,
                5.0, 1, 0, "", Collections.emptyList(), "255g", "8mm", "Cao", "Road",
                "", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptySet(), true, 1000L
        );
        List<CartItemModel> items = Arrays.asList(
                new CartItemModel("c1", shoe, "v_42", "42", "Cam", 2, true, 1000L),
                new CartItemModel("c2", shoe, "v_42", "42", "Cam", 1, true, 1000L)
        );
        Map<String, Integer> requested = OrderService.aggregateRequestedQuantities(items);
        String key = OrderService.buildVariantKey("p1", "v_42");
        assertEquals(Integer.valueOf(3), requested.get(key));

        Map<String, String> labels = Collections.singletonMap(key, "Novablast 5 (EU 42)");

        Map<String, Integer> enoughStock = Collections.singletonMap(key, 5);
        assertNull(OrderService.validateStockSufficiency(requested, enoughStock, labels));

        Map<String, Integer> lowStock = Collections.singletonMap(key, 2);
        String lowError = OrderService.validateStockSufficiency(requested, lowStock, labels);
        assertNotNull(lowError);
        assertTrue(lowError.contains("chỉ còn 2 đôi"));

        String missingError = OrderService.validateStockSufficiency(requested, new LinkedHashMap<>(), labels);
        assertNotNull(missingError);
        assertTrue(missingError.contains("không còn tồn tại"));
    }

    @Test
    public void orderCancelRestoresStockAndVoucherCountSafely() {
        assertTrue(OrderService.canCancelStatus("PENDING"));
        assertTrue(OrderService.canCancelStatus("pending"));
        assertFalse(OrderService.canCancelStatus("SHIPPING"));
        assertFalse(OrderService.canCancelStatus(null));

        assertEquals(7, OrderService.computeRestoredStock(5, 2));
        assertEquals(2L, OrderService.computeNextVoucherUsedCountOnCancel(3L));
        assertEquals(0L, OrderService.computeNextVoucherUsedCountOnCancel(0L));
    }

    @Test
    public void orderModelCanCancelFollowsStatus() {
        OrderModel pendingOrder = new OrderModel(
                "o1", "RN1", "u1", OrderModel.Status.PENDING, PaymentModel.Method.COD,
                Collections.emptyList(), "Addr", "Receiver", "Giao chuẩn", "",
                1_000_000L, 30_000L, 0L, 1_030_000L, 1000L
        );
        assertTrue(pendingOrder.canCancel());
        OrderModel shippingOrder = new OrderModel(
                "o2", "RN2", "u1", OrderModel.Status.SHIPPING, PaymentModel.Method.COD,
                Collections.emptyList(), "Addr", "Receiver", "Giao chuẩn", "",
                1_000_000L, 30_000L, 0L, 1_030_000L, 1000L
        );
        assertFalse(shippingOrder.canCancel());
    }

    @Test
    public void countOrdersByStatusHandlesNullEmptyAndMixedStatuses() {
        assertEquals(0, OrderService.countOrdersByStatus(null, OrderModel.Status.PENDING));
        assertEquals(0, OrderService.countOrdersByStatus(Collections.emptyList(), OrderModel.Status.SHIPPING));

        List<OrderModel> orders = Arrays.asList(
                buildStatusOrder("o1", OrderModel.Status.PENDING),
                null,
                buildStatusOrder("o2", OrderModel.Status.SHIPPING),
                buildStatusOrder("o3", OrderModel.Status.COMPLETED),
                buildStatusOrder("o4", OrderModel.Status.COMPLETED),
                buildStatusOrder("o5", OrderModel.Status.CANCELLED)
        );
        assertEquals(1, OrderService.countOrdersByStatus(orders, OrderModel.Status.PENDING));
        assertEquals(1, OrderService.countOrdersByStatus(orders, OrderModel.Status.SHIPPING));
        assertEquals(2, OrderService.countOrdersByStatus(orders, OrderModel.Status.COMPLETED));
        assertEquals(1, OrderService.countOrdersByStatus(orders, OrderModel.Status.CANCELLED));
    }

    @Test
    public void shippingFeeConstantsAndTotalCalculationHandleEdges() {
        assertEquals(30_000L, OrderService.STANDARD_SHIPPING_FEE);
        assertEquals(55_000L, OrderService.EXPRESS_SHIPPING_FEE);
        assertEquals(30_000L, OrderService.resolveShippingFee(false));
        assertEquals(55_000L, OrderService.resolveShippingFee(true));

        assertEquals(0L, OrderService.calculateSubtotal(null));
        assertEquals(0L, OrderService.calculateSubtotal(Collections.emptyList()));
        assertEquals(1_030_000L, OrderService.calculateTotal(1_000_000L, OrderService.resolveShippingFee(false), 0L));
        assertEquals(1_055_000L, OrderService.calculateTotal(1_000_000L, OrderService.resolveShippingFee(true), 0L));
        assertEquals(0L, OrderService.calculateTotal(50_000L, 30_000L, 200_000L));
        assertEquals(0L, OrderService.calculateTotal(-10_000L, -5_000L, 0L));
    }

    private OrderModel buildStatusOrder(String id, OrderModel.Status status) {
        return new OrderModel(
                id, id, "u1", status, PaymentModel.Method.COD,
                Collections.emptyList(), "Addr", "Receiver", OrderService.SHIPPING_METHOD_STANDARD, "",
                1_000_000L, OrderService.STANDARD_SHIPPING_FEE, 0L, 1_030_000L, 1000L
        );
    }
}
