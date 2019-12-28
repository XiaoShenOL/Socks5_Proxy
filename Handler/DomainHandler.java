package Handler;

import DnsResolve.DnsResolver;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class DomainHandler implements Handler {
    private final Selector selector;
    private SocketChannel client;
    private SocketChannel proxyTarget;

    private int port;
    private InetSocketAddress hostAddress;

    public DomainHandler(Selector selector, SocketChannel client, String hostname, int port) throws IOException {
        this.selector = selector;
        this.client = client;
        this.port = port;

        /* Клиент ждет, пока не будет получен IP адрес */
        client.keyFor(selector).interestOps(0);

        DnsResolver.createDnsQuery(this.client.keyFor(selector), hostname + ".");
    }

    public void handleDns(Message dnsAnswer) throws IOException {
        Record[] ipAnswers = dnsAnswer.getSectionArray(Section.ANSWER);
        for (Record record : ipAnswers) {
            if (record instanceof ARecord) {
                try {
                    ARecord aRecord = (ARecord) record;
                    InetAddress adr = aRecord.getAddress();
                    hostAddress = new InetSocketAddress(adr, port);
                    proxyTarget = SocketChannel.open();
                    proxyTarget.configureBlocking(false);
                    proxyTarget.connect(this.hostAddress);

                    proxyTarget.register(selector, SelectionKey.OP_CONNECT, this);
                    return;
                } catch (IOException e) {
                    client.keyFor(selector).cancel();
                    client.close();
                    proxyTarget.keyFor(selector).cancel();
                    proxyTarget.close();
                }
            }
        }
    }

    @Override
    public void onWrite(SelectableChannel channel) throws IOException {
        ByteBuffer clientReply = ByteBuffer.allocate(255);
        clientReply.put((byte) 0x05);
        clientReply.put((byte) 0x00);
        clientReply.put((byte) 0x00);
        clientReply.put((byte) 0x01);
        clientReply.put(hostAddress.getAddress().getAddress());
        clientReply.putShort((short) port);
        clientReply.flip();

        try {
            client.write(clientReply);
            MainWorkHandler handler = new MainWorkHandler(selector, client, proxyTarget);
            client.register(selector, SelectionKey.OP_READ, handler);
            proxyTarget.register(selector, SelectionKey.OP_READ, handler);
        } catch (Exception e) {
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

            client.keyFor(selector).interestOps(SelectionKey.OP_WRITE);
            proxyTarget.keyFor(selector).interestOps(0);
        } catch (IOException e) {
            client.keyFor(selector).cancel();
            client.close();
            proxyTarget.keyFor(selector).cancel();
            proxyTarget.close();
        }
    }

    @Override
    public void onRead(SelectableChannel channel) {
        throw new RuntimeException();
    }

    @Override
    public void onAccept(SelectableChannel channel) {
        throw new RuntimeException();
    }
}
