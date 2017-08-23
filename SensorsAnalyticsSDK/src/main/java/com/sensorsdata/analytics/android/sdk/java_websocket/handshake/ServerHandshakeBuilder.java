package com.sensorsdata.analytics.android.sdk.java_websocket.handshake;

public interface ServerHandshakeBuilder extends HandshakeBuilder, ServerHandshake {
    public void setHttpStatus(short status);

    public void setHttpStatusMessage(String message);
}
