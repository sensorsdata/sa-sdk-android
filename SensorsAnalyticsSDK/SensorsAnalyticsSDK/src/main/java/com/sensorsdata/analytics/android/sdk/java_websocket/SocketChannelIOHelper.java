package com.sensorsdata.analytics.android.sdk.java_websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import com.sensorsdata.analytics.android.sdk.java_websocket.WebSocket.Role;

public class SocketChannelIOHelper {

    public static boolean read(final ByteBuffer buf, WebSocketImpl ws, ByteChannel channel) throws IOException {
        buf.clear();
        int read = channel.read(buf);
        buf.flip();

        if (read == -1) {
            ws.eot();
            return false;
        }
        return read != 0;
    }

    public static boolean readMore(final ByteBuffer buf, WebSocketImpl ws, WrappedByteChannel channel) throws IOException {
        buf.clear();
        int read = channel.readMore(buf);
        buf.flip();

        if (read == -1) {
            ws.eot();
            return false;
        }
        return channel.isNeedRead();
    }

    public static boolean batch(WebSocketImpl ws, ByteChannel sockchannel) throws IOException {
        ByteBuffer buffer = ws.outQueue.peek();
        WrappedByteChannel c = null;

        if (buffer == null) {
            if (sockchannel instanceof WrappedByteChannel) {
                c = (WrappedByteChannel) sockchannel;
                if (c.isNeedWrite()) {
                    c.writeMore();
                }
            }
        } else {
            do {// FIXME writing as much as possible is unfair!!
                /*int written = */
                sockchannel.write(buffer);
                if (buffer.remaining() > 0) {
                    return false;
                } else {
                    ws.outQueue.poll(); // Buffer finished. Remove it.
                    buffer = ws.outQueue.peek();
                }
            } while (buffer != null);
        }

        if (ws != null && ws.outQueue.isEmpty() && ws.isFlushAndClose() && ws.getDraft() != null && ws.getDraft().getRole() != null && ws.getDraft().getRole() == Role.SERVER) {//
            synchronized (ws) {
                ws.closeConnection();
            }
        }
        return c != null ? !((WrappedByteChannel) sockchannel).isNeedWrite() : true;
    }
}
