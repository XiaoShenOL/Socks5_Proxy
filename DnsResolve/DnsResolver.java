package DnsResolve;

import Handler.DomainHandler;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;

public class DnsResolver {
    private static DatagramChannel dns;
    private static HashMap<Integer, SelectionKey> ipWaiters = new HashMap<>();

    private static int currentDnsID = 1;

    public static void createDnsResolver(Selector selector) throws IOException {
        dns = DatagramChannel.open();
        dns.configureBlocking(false);
        dns.connect(new InetSocketAddress("8.8.8.8", 53));
        dns.register(selector, SelectionKey.OP_READ);
    }

    public static void createDnsQuery(SelectionKey clientKey, String hostname) throws IOException {
        Name name = new Name(hostname);
        Record record = Record.newRecord(name, Type.A, DClass.IN);

        Message message = Message.newQuery(record);
        message.getHeader().setID(currentDnsID);

        ipWaiters.put(currentDnsID++, clientKey);

        dns.write(ByteBuffer.wrap(message.toWire()));
    }

    public static void handleDnsAnswer() throws IOException {
        ByteBuffer receivedData = ByteBuffer.allocate(4096);

        int recvBytesCount = dns.read(receivedData);

        byte[] receivedDataInBytes = new byte[recvBytesCount];
        System.arraycopy(receivedData.array(), 0, receivedDataInBytes, 0, recvBytesCount);

        Message dnsAnswer = new Message(receivedDataInBytes);

        SelectionKey clientKey = ipWaiters.get(dnsAnswer.getHeader().getID());
        ((DomainHandler) clientKey.attachment()).handleDns(dnsAnswer);
    }

    public static DatagramChannel getDnsChannel() {
        return dns;
    }
}
