package com.sensorsdata.analytics.android.sdk.java_websocket;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;

import com.sensorsdata.analytics.android.sdk.java_websocket.drafts.Draft;
import com.sensorsdata.analytics.android.sdk.java_websocket.framing.Framedata;
import com.sensorsdata.analytics.android.sdk.java_websocket.framing.Framedata.Opcode;

public interface WebSocket {
    public enum Role {
        CLIENT, SERVER
    }

    public enum READYSTATE {
        NOT_YET_CONNECTED, CONNECTING, OPEN, CLOSING, CLOSED;
    }

    public static final int DEFAULT_PORT = 80;

    public static final int DEFAULT_WSS_PORT = 443;

    public void close(int code, String message);

    public void close(int code);

    public void close();

    public abstract void closeConnection(int code, String message);

    public abstract void send(String text) throws NotYetConnectedException;

    public abstract void send(ByteBuffer bytes) throws IllegalArgumentException, NotYetConnectedException;

    public abstract void send(byte[] bytes) throws IllegalArgumentException, NotYetConnectedException;

    public abstract void sendFrame(Framedata framedata);

    public abstract void sendFragmentedFrame(Opcode op, ByteBuffer buffer, boolean fin);

    public abstract boolean hasBufferedData();

    public abstract InetSocketAddress getRemoteSocketAddress();

    public abstract InetSocketAddress getLocalSocketAddress();

    public abstract boolean isConnecting();

    public abstract boolean isOpen();

    public abstract boolean isClosing();

    public abstract boolean isFlushAndClose();

    public abstract boolean isClosed();

    public abstract Draft getDraft();

    public abstract READYSTATE getReadyState();

    public abstract String getResourceDescriptor();
}