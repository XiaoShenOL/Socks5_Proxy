package SocksProxyUtil;

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.Selector;

public class Buffer {
    private final int BUFFER_SIZE = 4096;

    private Selector selector;

    private SelectableChannel client;
    private ByteBuffer clientBuffer;
    private Attachment clientAttachment;

    private SelectableChannel proxyTarget;
    private ByteBuffer proxyTargetBuffer;
    private Attachment serverAttachment;

    public Buffer(Selector selector, SelectableChannel client, SelectableChannel proxyTarget) {
        this.selector = selector;

        this.client = client;
        clientBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        this.proxyTarget = proxyTarget;
        proxyTargetBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

    public Attachment getClientAttachment() {
        if (clientAttachment == null) {
            clientAttachment = new Attachment(client, selector) {
                @Override
                public Attachment getOtherAttachment() {
                    return getHostAttachment();
                }

                @Override
                public ByteBuffer getBuffer() {
                    return clientBuffer;
                }
            };
        }

        return clientAttachment;
    }

    public Attachment getHostAttachment() {
        if (serverAttachment == null) {
            serverAttachment = new Attachment(proxyTarget, selector) {
                @Override
                public Attachment getOtherAttachment() {
                    return getClientAttachment();
                }

                @Override
                public ByteBuffer getBuffer() {
                    return proxyTargetBuffer;
                }
            };
        }

        return serverAttachment;
    }
}