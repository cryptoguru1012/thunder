package network.thunder.core.etc;

import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.database.objects.PaymentWrapper;
import org.bitcoinj.core.ECKey;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class LNPaymentDBHandlerMock extends DBHandlerMock {
    public static final long INITIAL_AMOUNT_CHANNEL = 10000000;
    List<PaymentWrapper> payments = new ArrayList<>();
    List<PaymentSecret> secrets = new ArrayList<>();

    @Override
    public Channel getChannel (int id) {
        Channel channel = new Channel();
        channel.id = id;
        channel.channelStatus = new ChannelStatus();
        channel.channelStatus.amountServer = INITIAL_AMOUNT_CHANNEL;
        channel.channelStatus.amountClient = INITIAL_AMOUNT_CHANNEL;
        return channel;
    }

    @Override
    public List<Channel> getChannel (ECKey nodeKey) {
        List<Channel> list = new ArrayList<>();
        Channel c = getChannel(1);
        c.nodeKeyClient = nodeKey.getPubKey();
        list.add(c);
        return list;
    }

    @Override
    public List<Channel> getOpenChannel (ECKey nodeKey) {
        return getChannel(nodeKey);
    }

    @Override
    public void addPayment (PaymentWrapper paymentWrapper) {
        if (payments.contains(paymentWrapper)) {
            throw new RuntimeException("Double payment added?");
        }
        payments.add(paymentWrapper);
    }

    @Override
    public void updatePayment (PaymentWrapper paymentWrapper) {
        for (PaymentWrapper p : payments) {
            if (p.equals(paymentWrapper)) {
                p.paymentData = paymentWrapper.paymentData;
                p.receiver = paymentWrapper.receiver;
                p.sender = paymentWrapper.sender;
                p.statusReceiver = paymentWrapper.statusReceiver;
                p.statusSender = paymentWrapper.statusSender;
            }
        }
    }

    @Override
    public void updatePaymentSender (PaymentWrapper paymentWrapper) {
        for (PaymentWrapper p : payments) {
            if (p.equals(paymentWrapper)) {
                p.paymentData = paymentWrapper.paymentData;
                p.statusSender = paymentWrapper.statusSender;
            }
        }
    }

    @Override
    public void updatePaymentReceiver (PaymentWrapper paymentWrapper) {
        for (PaymentWrapper p : payments) {
            if (p.equals(paymentWrapper)) {
                p.paymentData = paymentWrapper.paymentData;
                p.statusReceiver = paymentWrapper.statusReceiver;
            }
        }
    }

    @Override
    public void updatePaymentAddReceiverAddress (PaymentSecret secret, byte[] receiver) {
        for (PaymentWrapper p : payments) {
            if (p.paymentData.secret.equals(secret)) {
                p.receiver = receiver;
            }
        }
    }

    @Override
    public PaymentWrapper getPayment (PaymentSecret paymentSecret) {
        for (PaymentWrapper payment : payments) {
            if (payment.paymentData.secret.equals(paymentSecret)) {
                return payment;
            }
        }
        return null;
    }

    @Override
    public void addPaymentSecret (PaymentSecret secret) {
        if (secrets.contains(secret)) {
            PaymentSecret oldSecret = secrets.get(secrets.indexOf(secret));
            oldSecret.secret = secret.secret;
        } else {
            secrets.add(secret);
        }
    }

    @Override
    public PaymentSecret getPaymentSecret (PaymentSecret secret) {
        if (!secrets.contains(secret)) {
            return null;
        }
        return secrets.get(secrets.indexOf(secret));
    }

    @Override
    public byte[] getSenderOfPayment (PaymentSecret paymentSecret) {
        for (PaymentWrapper payment : payments) {
            if (payment.paymentData.secret.equals(paymentSecret)) {
                return payment.sender;
            }
        }
        return null;
    }

    @Override
    public byte[] getReceiverOfPayment (PaymentSecret paymentSecret) {
        for (PaymentWrapper payment : payments) {
            if (payment.paymentData.secret.equals(paymentSecret)) {
                return payment.receiver;
            }
        }
        return null;
    }

    @Override
    public RevocationHash createRevocationHash (Channel channel) {
        byte[] secret = new byte[20];
        new SecureRandom().nextBytes(secret);
        byte[] secretHash = Tools.hashSecret(secret);
        RevocationHash hash = new RevocationHash(1, 1, secret, secretHash);
        return hash;
    }

    @Override
    public List<RevocationHash> getOldRevocationHashes (Channel channel) {
        return new ArrayList<>();
    }
}
