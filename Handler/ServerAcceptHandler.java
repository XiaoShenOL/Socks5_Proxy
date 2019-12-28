package Handler;

import java.io.IOException;
import java.nio.channels.*;


public class ServerAcceptHandler implements Handler {
    private Selector selector;

    public ServerAcceptHandler(Selector selector) {
        this.selector = selector;
    }

    @Override
    public void onAccept(SelectableChannel channel) throws IOException {
        SocketChannel client = ((ServerSocketChannel) channel).accept();
        client.configureBlocking(false);

        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
        clientKey.attach(new AuthMethodHandler(selector, client));
    }

    @Override
    public void onRead(SelectableChannel channel) {
        throw new RuntimeException();
    }

    @Override
    public void onWrite(SelectableChannel channel) {
        throw new RuntimeException();
    }

    @Override
    public void onConnect(SelectableChannel channel) {
        throw new RuntimeException();
    }
}
