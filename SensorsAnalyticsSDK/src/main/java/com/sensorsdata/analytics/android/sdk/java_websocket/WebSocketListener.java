package com.sensorsdata.analytics.android.sdk.java_websocket;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.sensorsdata.analytics.android.sdk.java_websocket.drafts.Draft;
import com.sensorsdata.analytics.android.sdk.java_websocket.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.java_websocket.framing.Framedata;
import com.sensorsdata.analytics.android.sdk.java_websocket.handshake.ClientHandshake;
import com.sensorsdata.analytics.android.sdk.java_websocket.handshake.Handshakedata;
import com.sensorsdata.analytics.android.sdk.java_websocket.handshake.ServerHandshake;
import com.sensorsdata.analytics.android.sdk.java_websocket.handshake.ServerHandshakeBuilder;

/**
 * Implemented by <tt>WebSocketClient</tt> and <tt>WebSocketServer</tt>.
 * The methods within are called by <tt>WebSocket</tt>.
 * Almost every method takes a first parameter conn which represents the source of the respective event.
 */
public interface WebSocketListener {


    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException;

    public void onWebsocketHandshakeReceivedAsClient(WebSocket conn, ClientHandshake request, ServerHandshake response) throws InvalidDataException;

    public void onWebsocketHandshakeSentAsClient(WebSocket conn, ClientHandshake request) throws InvalidDataException;

    public void onWebsocketMessage(WebSocket conn, String message);

    public void onWebsocketMessage(WebSocket conn, ByteBuffer blob);

    public void onWebsocketMessageFragment(WebSocket conn, Framedata frame);

    public void onWebsocketOpen(WebSocket conn, Handshakedata d);

    public void onWebsocketClose(WebSocket ws, int code, String reason, boolean remote);

    public void onWebsocketClosing(WebSocket ws, int code, String reason, boolean remote);

    public void onWebsocketCloseInitiated(WebSocket ws, int code, String reason);

    public void onWebsocketError(WebSocket conn, Exception ex);

    public void onWebsocketPing(WebSocket conn, Framedata f);

    public void onWebsocketPong(WebSocket conn, Framedata f);

    public String getFlashPolicy(WebSocket conn) throws InvalidDataException;

    public void onWriteDemand(WebSocket conn);

    public InetSocketAddress getLocalSocketAddress(WebSocket conn);

    public InetSocketAddress getRemoteSocketAddress(WebSocket conn);
}
