package SocksProxyUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public abstract class Attachment {
    private Selector selector;
    private SelectableChannel socketChannel;

    private boolean isFinishRead = false;
    private boolean outputShutdown = false;

    Attachment(SelectableChannel socketChannel, Selector selector){
        this.socketChannel = socketChannel;
        this.selector = selector;
    }

    public abstract Attachment getOtherAttachment();
    public abstract ByteBuffer getBuffer();

    public void addOption(int option) {
        SelectionKey currentOption = socketChannel.keyFor(selector);
        currentOption.interestOps(currentOption.interestOps()|option);
    }

    public void deleteOption(int option){
        SelectionKey currentOption = socketChannel.keyFor(selector);
        currentOption.interestOps(currentOption.interestOps()&~option);
    }

    public SelectableChannel getChannel() {
        return socketChannel;
    }


    public boolean isFinishRead() {
        return isFinishRead;
    }
    public void setFinishRead(boolean finishRead) {
        isFinishRead = finishRead;
    }

    public boolean isOutputShutdown() {
        return outputShutdown;
    }
    public void setOutputShutdown(boolean outputShutdown) {
        this.outputShutdown = outputShutdown;
    }


    public void close() throws IOException {
        socketChannel.keyFor(selector).cancel();
        socketChannel.close();
    }
}