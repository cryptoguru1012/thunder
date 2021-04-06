package network.thunder.core.database.objects;

import network.thunder.core.communication.layer.high.payments.PaymentData;

import static network.thunder.core.database.objects.PaymentStatus.EMBEDDED;
import static network.thunder.core.database.objects.PaymentStatus.UNKNOWN;

public class PaymentWrapper {

    public PaymentData paymentData;

    public PaymentStatus statusSender;
    public PaymentStatus statusReceiver;

    public byte[] sender;
    public byte[] receiver;

    public PaymentWrapper (byte[] sender, PaymentData paymentData) {
        this.sender = sender;
        this.paymentData = paymentData;

        this.statusReceiver = EMBEDDED;
        this.statusSender = UNKNOWN;
    }

    @Override
    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PaymentWrapper that = (PaymentWrapper) o;

        return paymentData != null ? paymentData.equals(that.paymentData) : that.paymentData == null;

    }

    @Override
    public int hashCode () {
        return paymentData != null ? paymentData.hashCode() : 0;
    }
}
