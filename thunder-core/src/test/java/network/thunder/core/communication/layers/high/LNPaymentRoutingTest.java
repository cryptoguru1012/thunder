package network.thunder.core.communication.layers.high;

import io.netty.channel.embedded.EmbeddedChannel;
import network.thunder.core.communication.layer.Message;
import network.thunder.core.communication.layer.ProcessorHandler;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.high.payments.messages.LNPaymentMessageFactory;
import network.thunder.core.communication.layer.high.payments.LNOnionHelper;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.LNPaymentProcessorImpl;
import network.thunder.core.communication.layer.high.payments.LNPaymentLogic;
import network.thunder.core.database.DBHandler;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.etc.*;
import network.thunder.core.communication.LNConfiguration;
import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.ServerObject;
import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;

import java.beans.PropertyVetoException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNull;

public class LNPaymentRoutingTest {

    public static final int TIME_TO_WAIT_FOR_FULL_EXCHANGE = 600;
    EmbeddedChannel channel12;
    EmbeddedChannel channel21;
    EmbeddedChannel channel23;
    EmbeddedChannel channel32;

    ServerObject node1 = new ServerObject();
    ServerObject node2 = new ServerObject();
    ServerObject node3 = new ServerObject();

    ClientObject node12 = new ClientObject(node1);
    ClientObject node21 = new ClientObject(node2);
    ClientObject node23 = new ClientObject(node2);
    ClientObject node32 = new ClientObject(node3);

    LNPaymentDBHandlerMock dbHandler1 = new LNPaymentDBHandlerMock();
    LNPaymentDBHandlerMock dbHandler2 = new LNPaymentDBHandlerMock();
    LNPaymentDBHandlerMock dbHandler3 = new LNPaymentDBHandlerMock();

    ContextFactory contextFactory1 = new MockLNPaymentContextFactory(node1, dbHandler1);
    ContextFactory contextFactory2 = new MockLNPaymentContextFactory(node2, dbHandler2);
    ContextFactory contextFactory3 = new MockLNPaymentContextFactory(node3, dbHandler3);

    LNPaymentProcessorImpl processor12;
    LNPaymentProcessorImpl processor21;
    LNPaymentProcessorImpl processor23;
    LNPaymentProcessorImpl processor32;

    LNConfiguration configuration = new LNConfiguration();

    @Before
    public void prepare () throws PropertyVetoException, SQLException {
        node12.name = "LNPayment12";
        node21.name = "LNPayment21";
        node23.name = "LNPayment23";
        node32.name = "LNPayment32";

        node12.pubKeyClient = node2.pubKeyServer;
        node21.pubKeyClient = node1.pubKeyServer;
        node23.pubKeyClient = node3.pubKeyServer;
        node32.pubKeyClient = node2.pubKeyServer;

        processor12 = new LNPaymentProcessorImpl(contextFactory1, dbHandler1, node12);
        processor21 = new LNPaymentProcessorImpl(contextFactory2, dbHandler2, node21);
        processor23 = new LNPaymentProcessorImpl(contextFactory2, dbHandler2, node23);
        processor32 = new LNPaymentProcessorImpl(contextFactory3, dbHandler3, node32);

        channel12 = new EmbeddedChannel(new ProcessorHandler(processor12, "LNPayment12"));
        channel21 = new EmbeddedChannel(new ProcessorHandler(processor21, "LNPayment21"));
        channel23 = new EmbeddedChannel(new ProcessorHandler(processor23, "LNPayment23"));
        channel32 = new EmbeddedChannel(new ProcessorHandler(processor32, "LNPayment32"));

        Message m = (Message) channel21.readOutbound();
        assertNull(m);

    }

    public void after () {
        channel12.checkException();
        channel21.checkException();

        channel21.checkException();
        channel23.checkException();

        channel32.checkException();
    }

    @Test
    public void exchangePaymentWithRouting () throws InterruptedException {
        OnionObject onionObject = getOnionObject(contextFactory1.getOnionHelper());
        PaymentData paymentData = getMockPaymentData();
        PaymentWrapper wrapper = new PaymentWrapper(new byte[0], paymentData);

        dbHandler1.addPayment(wrapper);
        dbHandler3.addPaymentSecret(paymentData.secret);

        paymentData.secret.secret = null;

        paymentData.onionObject = onionObject;
        processor12.makePayment(paymentData);

        connectChannel(channel12, channel21);
        connectChannel(channel23, channel32);

        Thread.sleep(TIME_TO_WAIT_FOR_FULL_EXCHANGE);

        ChannelStatus status12 = processor12.getStatusTemp();
        ChannelStatus status21 = processor21.getStatusTemp();
        ChannelStatus status23 = processor23.getStatusTemp();
        ChannelStatus status32 = processor21.getStatusTemp();

        assertEquals(status12.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL + paymentData.amount);
        assertEquals(status12.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL - paymentData.amount);

        assertEquals(status21.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL - paymentData.amount);
        assertEquals(status21.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL + paymentData.amount);

        assertEquals(status23.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL + paymentData.amount);
        assertEquals(status23.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL - paymentData.amount);

        assertEquals(status32.amountClient, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL - paymentData.amount);
        assertEquals(status32.amountServer, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL + paymentData.amount);
        after();
    }

    @Test
    public void exchangePaymentWithRoutingRefund () throws InterruptedException {
        OnionObject onionObject = getOnionObject(contextFactory1.getOnionHelper());
        PaymentData paymentData = getMockPaymentData();
        PaymentWrapper wrapper = new PaymentWrapper(new byte[0], paymentData);

        dbHandler1.addPayment(wrapper);

        paymentData.secret.secret = null;

        paymentData.onionObject = onionObject;
        processor12.makePayment(paymentData);

        connectChannel(channel12, channel21);
        connectChannel(channel23, channel32);

        Thread.sleep(TIME_TO_WAIT_FOR_FULL_EXCHANGE);
        testUnchangedChannelAmounts();
        after();
    }

    @Test
    public void exchangePaymentWithRoutingCorruptedOnionObject () throws InterruptedException {
        OnionObject onionObject = getOnionObject(contextFactory1.getOnionHelper());

        Tools.copyRandomByteInByteArray(onionObject.data, 100, 2);

        PaymentData paymentData = getMockPaymentData();
        PaymentWrapper wrapper = new PaymentWrapper(new byte[0], paymentData);

        dbHandler1.addPayment(wrapper);

        paymentData.secret.secret = null;

        paymentData.onionObject = onionObject;
        processor12.makePayment(paymentData);

        connectChannel(channel12, channel21);
        connectChannel(channel23, channel32);

        Thread.sleep(TIME_TO_WAIT_FOR_FULL_EXCHANGE);
        testUnchangedChannelAmounts();
        after();
    }

    @Test
    public void exchangePaymentWithRoutingToUnknownNode () throws InterruptedException {
        OnionObject onionObject = getOnionObject(contextFactory1.getOnionHelper(), node12.pubKeyClient.getPubKey(), new ECKey().getPubKey());

        PaymentData paymentData = getMockPaymentData();
        PaymentWrapper wrapper = new PaymentWrapper(new byte[0], paymentData);

        dbHandler1.addPayment(wrapper);

        paymentData.secret.secret = null;

        paymentData.onionObject = onionObject;
        processor12.makePayment(paymentData);

        connectChannel(channel12, channel21);
        connectChannel(channel23, channel32);

        Thread.sleep(TIME_TO_WAIT_FOR_FULL_EXCHANGE);
        testUnchangedChannelAmounts();
        after();
    }

    @Test
    public void exchangePaymentWithTooHighPayment () throws InterruptedException {
        long normalAmount = LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL;
        long doubleAmount = normalAmount * 2;
        ChannelStatus status12 = processor12.getChannel().channelStatus;
        status12.amountClient *= 2;
        status12.amountServer *= 2;

        ChannelStatus status21 = processor21.getChannel().channelStatus;
        status21.amountClient *= 2;
        status21.amountServer *= 2;

        OnionObject onionObject = getOnionObject(contextFactory1.getOnionHelper());

        PaymentData paymentData = getMockPaymentData();
        paymentData.amount = LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL + 1;
        PaymentWrapper wrapper = new PaymentWrapper(new byte[0], paymentData);

        dbHandler1.addPayment(wrapper);

        paymentData.secret.secret = null;

        paymentData.onionObject = onionObject;
        processor12.makePayment(paymentData);

        connectChannel(channel12, channel21);
        connectChannel(channel23, channel32);

        Thread.sleep(TIME_TO_WAIT_FOR_FULL_EXCHANGE);

        testUnchangedChannelAmounts(doubleAmount, doubleAmount, normalAmount, normalAmount);
        after();
    }

    public void testUnchangedChannelAmounts () {
        testUnchangedChannelAmounts(LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL,
                LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL, LNPaymentDBHandlerMock.INITIAL_AMOUNT_CHANNEL);
    }

    public void testUnchangedChannelAmounts (long amount12, long amount21, long amount23, long amount32) {
        ChannelStatus status12 = processor12.getStatusTemp();
        ChannelStatus status21 = processor21.getStatusTemp();
        ChannelStatus status23 = processor23.getStatusTemp();
        ChannelStatus status32 = processor32.getStatusTemp();

        assertEquals(status12.amountClient, amount12);
        assertEquals(status12.amountServer, amount12);

        assertEquals(status21.amountClient, amount21);
        assertEquals(status21.amountServer, amount21);

        assertEquals(status23.amountClient, amount23);
        assertEquals(status23.amountServer, amount23);

        assertEquals(status32.amountClient, amount32);
        assertEquals(status32.amountServer, amount32);
    }

    public static void exchangeMessages (EmbeddedChannel from, EmbeddedChannel to) {
        Object message = from.readOutbound();
        if (message != null) {
            to.writeInbound(message);
        }
    }

    public static void exchangeMessagesDuplex (EmbeddedChannel from, EmbeddedChannel to) {
        exchangeMessages(from, to);
        exchangeMessages(to, from);
    }

    public OnionObject getOnionObject (LNOnionHelper onionHelper) {
        return getOnionObject(onionHelper, node12.pubKeyClient.getPubKey(), node23.pubKeyClient.getPubKey());
    }

    public OnionObject getOnionObject (LNOnionHelper onionHelper, byte[] node2, byte[] node3) {
        List<byte[]> route = new ArrayList<>();
        route.add(node2);
        route.add(node3);
        return onionHelper.createOnionObject(route, null);
    }

    public PaymentData getMockPaymentData () {
        PaymentData paymentData = new PaymentData();
        paymentData.sending = true;
        paymentData.amount = 10000;
        paymentData.secret = new PaymentSecret(Tools.getRandomByte(20));

        paymentData.timestampOpen = Tools.currentTime();
        paymentData.timestampRefund = Tools.currentTime() + 3
                * configuration.MAX_REFUND_DELAY * configuration.MAX_OVERLAY_REFUND;
        paymentData.csvDelay = configuration.DEFAULT_REVOCATION_DELAY;

        return paymentData;
    }

    public void connectChannel (EmbeddedChannel from, EmbeddedChannel to) {
        new Thread(() -> {
            while (true) {
                exchangeMessagesDuplex(from, to);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    class MockLNPaymentContextFactory extends MockContextFactory {

        public MockLNPaymentContextFactory (ServerObject node, DBHandler dbHandler) {
            super(node, dbHandler);
        }

        @Override
        public LNPaymentLogic getLNPaymentLogic () {
            return new MockLNPaymentLogic(getLNPaymentMessageFactory());
        }

        @Override
        public LNPaymentMessageFactory getLNPaymentMessageFactory () {
            return new LNPaymentMessageFactoryMock();
        }
    }

}