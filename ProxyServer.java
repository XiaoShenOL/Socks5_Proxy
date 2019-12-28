import DnsResolve.DnsResolver;
import Handler.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Set;

public class ProxyServer implements AutoCloseable {
    private Selector selector;
    private ServerSocketChannel serverChannel;

    public ProxyServer(int port) throws IOException {
        selector = Selector.open();

        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(port));

        SelectionKey serverKey = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        serverKey.attach(new ServerAcceptHandler(selector));
    }

    public void start() throws IOException {
        DnsResolver.createDnsResolver(selector);

        while (true) {
            selector.select();

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if (key.isValid() && key.isAcceptable())
                    accept(key);

                if (key.isValid() && key.isConnectable())
                    connect(key);

                if (key.isValid() && key.isReadable())
                    read(key);

                if (key.isValid() && key.isWritable())
                    write(key);
            }
        }
    }

    private void read(SelectionKey key) throws IOException {
        if (key.channel() == DnsResolver.getDnsChannel()) {
            DnsResolver.handleDnsAnswer();
            return;
        }

        Handler keyHandler = (Handler) key.attachment();
        keyHandler.onRead(key.channel());
    }

    private void write(SelectionKey key) throws IOException {
        Handler keyHandler = (Handler) key.attachment();
        keyHandler.onWrite(key.channel());
    }

    private void accept(SelectionKey key) throws IOException {
        Handler keyHandler = (Handler) key.attachment();
        keyHandler.onAccept(key.channel());
    }

    private void connect(SelectionKey key) throws IOException {
        Handler keyHandler = (Handler) key.attachment();
        keyHandler.onConnect(key.channel());
    }

    @Override
    public void close() throws Exception {
        selector.close();
        serverChannel.close();
    }
}
