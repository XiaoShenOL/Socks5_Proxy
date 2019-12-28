package Handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class AuthMethodHandler implements Handler {
    private Selector selector;
    private SocketChannel client;

    private ByteBuffer buffer;

    public AuthMethodHandler(Selector selector, SocketChannel client) {
        this.selector = selector;
        this.client = client;
        this.buffer = ByteBuffer.allocate(255);
    }

    @Override
    public void onRead(SelectableChannel channel) throws IOException {
        try {
            int readCount = client.read(buffer);
            if (readCount < 0) {
                throw new IOException("Cannot read auth message");
            }

            buffer.flip();

            byte protocolVersion = buffer.get();
            byte authMethodsCount = buffer.get();
            if (protocolVersion != 0x05 || authMethodsCount != 0x01 || buffer.get() != 0x00) {
                throw new IOException("Bad auth method request");
            }

            client.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
        } catch (IOException e) {
            client.keyFor(selector).cancel();
            client.close();
        }
    }

    @Override
    public void onWrite(SelectableChannel channel) throws IOException {
        try {
            buffer.clear();

            /* 0x05 - номер протокола, 0x00 - выбранный метод аутентификации */
            byte[] serverAnswer = {0x05, 0x00};
            buffer.put(serverAnswer);
            buffer.flip();

            client.write(buffer);

            SelectionKey clientKey = channel.keyFor(selector);
            clientKey.interestOps(SelectionKey.OP_READ);
            clientKey.attach(new ClientOperationHandler(selector, client));
        } catch (IOException e) {
            client.keyFor(selector).cancel();
            client.close();
        }
    }

    @Override
    public void onAccept(SelectableChannel channel) {
        throw new RuntimeException();
    }

    @Override
    public void onConnect(SelectableChannel channel) {
        throw new RuntimeException();
    }
}
