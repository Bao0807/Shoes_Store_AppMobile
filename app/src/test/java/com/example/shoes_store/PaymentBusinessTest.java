package com.example.shoes_store;

import com.example.shoes_store.module.payment.PaymentModel;
import com.example.shoes_store.module.payment.PaymentService;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
    public void paymentEnumsAndModelStatusWorkAsExpected() {
        assertEquals(PaymentModel.Method.BANK_QR, PaymentModel.Method.fromStorage("BANK_QR"));
        assertEquals(PaymentModel.Method.E_WALLET, PaymentModel.Method.fromStorage("Ví điện tử"));
        assertEquals(PaymentModel.Method.COD, PaymentModel.Method.fromStorage(null));

        assertEquals(PaymentModel.Status.REFUNDED, PaymentModel.Status.fromStorage("refunded"));
        assertEquals(PaymentModel.Status.UNPAID, PaymentModel.Status.fromStorage(null));

        PaymentModel paid = new PaymentModel(
                "ord_1", PaymentModel.Status.PAID, "TXN-RN1", 1000L
        );
        PaymentModel unpaid = new PaymentModel(
                "ord_2", PaymentModel.Status.UNPAID, "", 1000L
        );
        assertEquals(PaymentModel.Status.PAID, paid.getStatus());
        assertEquals(PaymentModel.Status.UNPAID, unpaid.getStatus());
    }
}
