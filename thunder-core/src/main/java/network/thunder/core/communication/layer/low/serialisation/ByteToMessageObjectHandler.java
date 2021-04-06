package network.thunder.core.communication.layer.low.serialisation;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import network.thunder.core.communication.layer.Message;

import java.util.List;

public class ByteToMessageObjectHandler extends ByteToMessageDecoder {
    MessageSerializer serializater;

    public ByteToMessageObjectHandler (MessageSerializer serializater) {
        this.serializater = serializater;
    }

    @Override
    protected void decode (ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        try {
            if (in.readableBytes() > 0) {
                byte[] data = new byte[in.readableBytes()];
                in.readBytes(data);

                Message message = serializater.deserializeMessage(data);

                out.add(message);
//                System.out.println("Incoming: " + message);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
