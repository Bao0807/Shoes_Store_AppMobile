package com.example.shoes_store;

import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.payment.PaymentService;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PaymentBusinessTest {

    @Test
    public void resolveInitialStatusDependsOnPaymentMethod() {
        assertEquals(PaymentModel.Status.UNPAID, PaymentService.resolveInitialStatus(PaymentModel.Method.COD));
        assertEquals(PaymentModel.Status.PAID, PaymentService.resolveInitialStatus(PaymentModel.Method.BANK_QR));
        assertEquals(PaymentModel.Status.PAID, PaymentService.resolveInitialStatus(PaymentModel.Method.E_WALLET));
    }

    @Test
    public void resolveStatusOnOrderCancelHandlesPaidAndUnpaid() {
        assertEquals(PaymentModel.Status.REFUNDED, PaymentService.resolveStatusOnOrderCancel("PAID"));
        assertEquals(PaymentModel.Status.REFUNDED, PaymentService.resolveStatusOnOrderCancel("paid"));
        assertEquals(PaymentModel.Status.FAILED, PaymentService.resolveStatusOnOrderCancel("UNPAID"));
        assertEquals(PaymentModel.Status.FAILED, PaymentService.resolveStatusOnOrderCancel("FAILED"));
        assertEquals(PaymentModel.Status.FAILED, PaymentService.resolveStatusOnOrderCancel(null));
    }

    @Test
    public void buildTransactionCodeOnlyGeneratesForPaidStatus() {
        assertEquals("TXN-RN261123", PaymentService.buildTransactionCode(PaymentModel.Status.PAID, "RN261123"));
        assertEquals("", PaymentService.buildTransactionCode(PaymentModel.Status.UNPAID, "RN261123"));
        assertEquals("", PaymentService.buildTransactionCode(PaymentModel.Status.FAILED, "RN261123"));
        assertEquals("", PaymentService.buildTransactionCode(PaymentModel.Status.REFUNDED, "RN261123"));
    }

    @Test
    public void paymentEnumsAndIsPaidWorkAsExpected() {
        assertEquals(PaymentModel.Method.BANK_QR, PaymentModel.Method.fromStorage("BANK_QR"));
        assertEquals(PaymentModel.Method.E_WALLET, PaymentModel.Method.fromStorage("Ví điện tử"));
        assertEquals(PaymentModel.Method.COD, PaymentModel.Method.fromStorage(null));

        assertEquals(PaymentModel.Status.REFUNDED, PaymentModel.Status.fromStorage("refunded"));
        assertEquals(PaymentModel.Status.UNPAID, PaymentModel.Status.fromStorage(null));

        PaymentModel paid = new PaymentModel(
                "pay_1", "ord_1", "u_1",
                PaymentModel.Method.E_WALLET, PaymentModel.Status.PAID,
                2_000_000L, "TXN-RN1", 1000L, 1000L
        );
        PaymentModel unpaid = new PaymentModel(
                "pay_2", "ord_2", "u_1",
                PaymentModel.Method.COD, PaymentModel.Status.UNPAID,
                2_000_000L, "", 0L, 1000L
        );
        assertTrue(paid.isPaid());
        assertFalse(unpaid.isPaid());
    }
}
