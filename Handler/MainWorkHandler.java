package Handler;

import SocksProxyUtil.Attachment;
import SocksProxyUtil.Buffer;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class MainWorkHandler implements Handler {
    private SocketChannel client;

    private Attachment clientAttachment;
    private Attachment hostAttachment;

    public MainWorkHandler(Selector selector, SocketChannel client, SocketChannel proxyTarget) {
        this.client = client;

        Buffer buffer = new Buffer(selector, client, proxyTarget);

        this.clientAttachment = buffer.getClientAttachment();
        this.hostAttachment = buffer.getHostAttachment();
    }

    @Override
    public void onRead(SelectableChannel channel) {
        Attachment attachment = getCurrentBuffer(channel);

        try {
            int byteRead = ((SocketChannel)attachment.getChannel()).read(attachment.getBuffer());
            SocketChannel otherChannel = (SocketChannel) attachment.getOtherAttachment().getChannel();

            if (byteRead > 0 && otherChannel.isConnected())
                attachment.getOtherAttachment().addOption(SelectionKey.OP_WRITE);

            if(byteRead == -1) {
                attachment.deleteOption(SelectionKey.OP_READ);
                attachment.setFinishRead(true);

                if(attachment.getBuffer().position() == 0) {
                    /* больше нечего отправлять */
                    otherChannel.shutdownOutput();
                    attachment.getOtherAttachment().setOutputShutdown(true);

                    /* Если закрыта еще одна сторона */
                    if(attachment.isOutputShutdown() || attachment.getOtherAttachment().getBuffer().position() == 0) {
                        attachment.close();
                        attachment.getOtherAttachment().close();
                    }
                }
            }
        } catch (IOException e) { }
    }

    @Override
    public void onWrite(SelectableChannel channel) {
        Attachment attachment = getCurrentBuffer(channel);
        Attachment otherAttachment = attachment.getOtherAttachment();
        otherAttachment.getBuffer().flip();

        try {
            SocketChannel socketChannel = (SocketChannel)attachment.getChannel();
            int byteWrite = socketChannel.write(otherAttachment.getBuffer());
            if (byteWrite > 0) {
                otherAttachment.getBuffer().compact();
                otherAttachment.addOption(SelectionKey.OP_READ);
            }

            if(otherAttachment.getBuffer().position() == 0) {
                attachment.deleteOption(SelectionKey.OP_WRITE);

                if (otherAttachment.isFinishRead()) {
                    socketChannel.shutdownOutput();
                    attachment.setOutputShutdown(true);

                    /* если выход другого канала закрыт, то нет смысла работать дальше */
                    if (otherAttachment.isOutputShutdown()) {
                        attachment.close();
                        otherAttachment.close();
                    }
                }
            }
        } catch (IOException e) { }
    }

    private Attachment getCurrentBuffer(SelectableChannel channel) {
        return channel.equals(client) ? clientAttachment : hostAttachment;
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
