package network.thunder.core.communication.layers.high;

import network.thunder.core.communication.layer.high.payments.LNOnionHelperImpl;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;
import network.thunder.core.communication.layer.high.payments.messages.PeeledOnion;
import network.thunder.core.communication.layer.high.payments.LNOnionHelper;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class LNOnionHelperImplTest {

    List<ECKey> keyList = new ArrayList<>();

    LNOnionHelper onionHelper = new LNOnionHelperImpl();

    @Before
    public void prepare () {

    }

    @Test
    public void shouldBuildAndDeconstructCorrectFullOnion () {
        buildKeylist(OnionObject.MAX_HOPS);

        OnionObject object = onionHelper.createOnionObject(getByteList(keyList), null);

        List<byte[]> listFromOnion = new ArrayList<>();
        listFromOnion.add(keyList.get(0).getPubKey());

        for (ECKey key : keyList) {
            LNOnionHelper helperTemp = new LNOnionHelperImpl();
            PeeledOnion peeledOnion = helperTemp.loadMessage(key, object);

            if (peeledOnion.isLastHop) {
                listFromOnion.add(key.getPubKey());
            } else {
                listFromOnion.add(peeledOnion.nextHop.getPubKey());
                object = peeledOnion.onionObject;
            }

        }

        for (int i = 0; i < keyList.size(); ++i) {
            ECKey key = keyList.get(i);
            byte[] keyOnion = listFromOnion.get(i);
            System.out.println(Tools.bytesToHex(key.getPubKey()) + " " + Tools.bytesToHex(keyOnion));
            assertTrue(Arrays.equals(key.getPubKey(), keyOnion));
        }
    }

    @Test
    public void shouldBuildAndDeconstructCorrectHalfOnion () {
        buildKeylist(OnionObject.MAX_HOPS - 4);

        OnionObject object = onionHelper.createOnionObject(getByteList(keyList), null);

        List<byte[]> listFromOnion = new ArrayList<>();
        listFromOnion.add(keyList.get(0).getPubKey());

        for (ECKey key : keyList) {
            LNOnionHelper helperTemp = new LNOnionHelperImpl();
            PeeledOnion peeledOnion = helperTemp.loadMessage(key, object);

            if (peeledOnion.isLastHop) {
                listFromOnion.add(key.getPubKey());
            } else {
                listFromOnion.add(peeledOnion.nextHop.getPubKey());
                object = peeledOnion.onionObject;
            }

        }

        for (int i = 0; i < keyList.size(); ++i) {
            ECKey key = keyList.get(i);
            byte[] keyOnion = listFromOnion.get(i);
            System.out.println(Tools.bytesToHex(key.getPubKey()) + " " + Tools.bytesToHex(keyOnion));
            assertTrue(Arrays.equals(key.getPubKey(), keyOnion));
        }
    }

    public void buildKeylist (int hops) {
        for (int i = 0; i < hops; ++i) {
            keyList.add(new ECKey());
            System.out.println(Tools.bytesToHex(keyList.get(i).getPubKey()));
        }
    }

    public static List<byte[]> getByteList (List<ECKey> keyList) {
        List<byte[]> byteList = new ArrayList<>();
        for (ECKey key : keyList) {
            byteList.add(key.getPubKey());
        }

        return byteList;
    }

}