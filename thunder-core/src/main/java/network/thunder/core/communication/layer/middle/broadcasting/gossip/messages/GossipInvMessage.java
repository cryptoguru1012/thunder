package network.thunder.core.communication.layer.middle.broadcasting.gossip.messages;

import com.google.common.base.Preconditions;
import network.thunder.core.etc.Tools;

import java.util.List;

public class GossipInvMessage implements Gossip {
    public List<byte[]> inventoryList;

    public GossipInvMessage (List<byte[]> inventoryList) {
        this.inventoryList = inventoryList;
    }

    @Override
    public void verify () {
        Preconditions.checkNotNull(inventoryList);
    }

    @Override
    public String toString () {
        return "GossipInvMessage{" +
                "size = " + inventoryList.size() + " - " + Tools.bytesToHex(inventoryList.get(0)) +
                '}';
    }
}
