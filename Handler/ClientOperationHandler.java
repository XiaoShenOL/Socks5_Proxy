package Handler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.*;

public class ClientOperationHandler implements Handler {
    private Selector selector;
    private SocketChannel client;
    private SocketChannel proxyTarget;

    private ByteBuffer buffer;

    public ClientOperationHandler(Selector selector, SocketChannel client) {
        this.selector = selector;
        this.client = client;

        buffer = ByteBuffer.allocate(255);
    }

    @Override
    public void onRead(SelectableChannel channel) throws IOException {
        try {
            int readCount = client.read(buffer);
            if (readCount < 0) {
                throw new IOException("Cannot read operation message");
            }

            buffer.flip();

            byte protocolVersion = buffer.get();
            byte operationNumber = buffer.get();
            if (protocolVersion != 0x05 || operationNumber != 0x01) {
                throw new IOException("Bad operation message");
            }

            /* зарезервированный байт протокола SOCKS5 */
            buffer.get();

            byte addressType = buffer.get();
            switch (addressType) {
                case 0x01: // IPv4
                    connectIPv4();
                    break;
                case 0x03: // DOMAIN
                    connectDomain();
                    break;
            }
        } catch (Exception e) {
            client.keyFor(selector).cancel();
            client.close();
        }
    }

    private void connectDomain() throws IOException {
        byte hostLength = buffer.get();
        byte[] addressInBytes = new byte[(int) hostLength & 0xFF];

        buffer.get(addressInBytes);
        String hostname = new String(addressInBytes);

        short port = buffer.getShort();

        try {
            client.register(selector, 0, new DomainHandler(selector, client, hostname, port));
        } catch (IOException e) {
            client.keyFor(selector).cancel();
            client.close();
        }
    }

    private void connectIPv4() throws IOException {
        try {
            byte[] ipAddress = new byte[4];
            buffer.get(ipAddress);

            buffer.order(ByteOrder.BIG_ENDIAN);
            int port = buffer.getShort();

            proxyTarget = SocketChannel.open();
            proxyTarget.configureBlocking(false);
            proxyTarget.connect(new InetSocketAddress(InetAddress.getByAddress(ipAddress), port));

            client.keyFor(selector).interestOps(0);
            proxyTarget.register(selector, SelectionKey.OP_CONNECT, this);
        } catch (IOException e) {
            client.keyFor(selector).cancel();
            client.close();
            proxyTarget.keyFor(selector).cancel();
            proxyTarget.close();
        }
    }


    @Override
    public void onWrite(SelectableChannel channel) throws IOException {
        try {
            buffer.flip();
            buffer.put(1, (byte) 0x00);

            client.write(buffer);

            MainWorkHandler handler = new MainWorkHandler(selector, client, proxyTarget);
            client.register(selector,SelectionKey.OP_READ, handler);
            proxyTarget.register(selector,SelectionKey.OP_READ, handler);
        } catch (IOException e) {
            client.keyFor(selector).cancel();
            client.close();
            proxyTarget.keyFor(selector).cancel();
            proxyTarget.close();
        }
    }

    @Override
    public void onConnect(SelectableChannel channel) throws IOException {
        try {
            SocketChannel socketChannel = (SocketChannel) channel;
            socketChannel.finishConnect();

            proxyTarget.keyFor(selector).interestOps(0);
            client.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        } catch (IOException e) {
            client.keyFor(selector).cancel();
            client.close();
            proxyTarget.keyFor(selector).cancel();
            proxyTarget.close();
        }
    }

    @Override
    public void onAccept(SelectableChannel channel) {
        throw new RuntimeException();
    }
}
