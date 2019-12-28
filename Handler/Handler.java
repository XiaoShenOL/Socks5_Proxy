package Handler;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

public interface Handler {
    void onRead(SelectableChannel channel) throws IOException;
    void onWrite(SelectableChannel channel) throws IOException;
    void onAccept(SelectableChannel channel) throws IOException;
    void onConnect(SelectableChannel channel) throws IOException;
}