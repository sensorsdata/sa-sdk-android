package com.sensorsdata.analytics.android.sdk.java_websocket.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import com.sensorsdata.analytics.android.sdk.java_websocket.WebSocket;
import com.sensorsdata.analytics.android.sdk.java_websocket.WebSocketAdapter;
import com.sensorsdata.analytics.android.sdk.java_websocket.WebSocketImpl;
import com.sensorsdata.analytics.android.sdk.java_websocket.drafts.Draft;
import com.sensorsdata.analytics.android.sdk.java_websocket.drafts.Draft_17;
import com.sensorsdata.analytics.android.sdk.java_websocket.exceptions.InvalidHandshakeException;
import com.sensorsdata.analytics.android.sdk.java_websocket.framing.CloseFrame;
import com.sensorsdata.analytics.android.sdk.java_websocket.framing.Framedata;
import com.sensorsdata.analytics.android.sdk.java_websocket.framing.Framedata.Opcode;
import com.sensorsdata.analytics.android.sdk.java_websocket.handshake.HandshakeImpl1Client;
import com.sensorsdata.analytics.android.sdk.java_websocket.handshake.Handshakedata;
import com.sensorsdata.analytics.android.sdk.java_websocket.handshake.ServerHandshake;

import android.util.Log;

/**
 * A subclass must implement at least <var>onOpen</var>, <var>onClose</var>, and <var>onMessage</var> to be
 * useful. At runtime the user is expected to establish a connection via {@link #connect()}, then receive events like {@link #onMessage(String)} via the overloaded methods and to {@link #send(String)} data to the server.
 */
public abstract class WebSocketClient extends WebSocketAdapter implements Runnable, WebSocket {

    protected URI uri = null;

    private WebSocketImpl engine = null;

    private Socket socket = null;

    private InputStream istream;

    private OutputStream ostream;

    private Proxy proxy = Proxy.NO_PROXY;

    private Thread writeThread;

    private Draft draft;

    private Map<String, String> headers;

    private CountDownLatch connectLatch = new CountDownLatch(1);

    private CountDownLatch closeLatch = new CountDownLatch(1);

    private int connectTimeout = 0;

    public WebSocketClient(URI serverURI) {
        this(serverURI, new Draft_17());
    }

    public WebSocketClient(URI serverUri, Draft draft) {
        this(serverUri, draft, null, 0);
    }

    public WebSocketClient(URI serverUri, Draft protocolDraft, Map<String, String> httpHeaders, int connectTimeout) {
        if (serverUri == null) {
            throw new IllegalArgumentException();
        } else if (protocolDraft == null) {
            throw new IllegalArgumentException("null as draft is permitted for `WebSocketServer` only!");
        }
        this.uri = serverUri;
        this.draft = protocolDraft;
        this.headers = httpHeaders;
        this.connectTimeout = connectTimeout;
        this.engine = new WebSocketImpl(this, protocolDraft);
    }

    public URI getURI() {
        return uri;
    }

    public Draft getDraft() {
        return draft;
    }

    public void connect() {
        if (writeThread != null)
            throw new IllegalStateException("WebSocketClient objects are not reuseable");
        writeThread = new Thread(this);
        writeThread.start();
    }

    public boolean connectBlocking() throws InterruptedException {
        connect();
        connectLatch.await();
        return engine.isOpen();
    }

    public void close() {
        if (writeThread != null) {
            engine.close(CloseFrame.NORMAL);
        }
    }

    public void closeBlocking() throws InterruptedException {
        close();
        closeLatch.await();
    }

    public void send(String text) throws NotYetConnectedException {
        engine.send(text);
    }

    public void send(byte[] data) throws NotYetConnectedException {
        engine.send(data);
    }

    public void run() {
        try {
            if (socket == null) {
                socket = new Socket(proxy);
            } else if (socket.isClosed()) {
                throw new IOException();
            }
            if (!socket.isBound())
                socket.connect(new InetSocketAddress(uri.getHost(), getPort()), connectTimeout);
            istream = socket.getInputStream();
            ostream = socket.getOutputStream();

            sendHandshake();
        } catch ( /*IOException | SecurityException | UnresolvedAddressException | InvalidHandshakeException | ClosedByInterruptException | SocketTimeoutException */Exception e) {
            onWebsocketError(engine, e);
            engine.closeConnection(CloseFrame.NEVER_CONNECTED, e.getMessage());
            return;
        }

        writeThread = new Thread(new WebsocketWriteThread());
        writeThread.start();

        byte[] rawbuffer = new byte[WebSocketImpl.RCVBUF];
        int readBytes;

        try {
            while (!isClosed() && !isClosing() && (readBytes = istream.read(rawbuffer)) != -1) {
                engine.decode(ByteBuffer.wrap(rawbuffer, 0, readBytes));
            }
            engine.eot();
        } catch (IOException e) {
            engine.eot();
        } catch (RuntimeException e) {
            // this catch case covers internal errors only and indicates a bug in this websocket implementation
            onError(e);
            engine.closeConnection(CloseFrame.ABNORMAL_CLOSE, e.getMessage());
        }
        assert (socket.isClosed());
    }

    private int getPort() {
        int port = uri.getPort();
        if (port == -1) {
            String scheme = uri.getScheme();
            if (scheme.equals("wss")) {
                return WebSocket.DEFAULT_WSS_PORT;
            } else if (scheme.equals("ws")) {
                return WebSocket.DEFAULT_PORT;
            } else {
                throw new RuntimeException("unkonow scheme" + scheme);
            }
        }
        return port;
    }

    private void sendHandshake() throws InvalidHandshakeException {
        String path;
        String part1 = uri.getPath();
        String part2 = uri.getQuery();
        if (part1 == null || part1.length() == 0)
            path = "/";
        else
            path = part1;
        if (part2 != null)
            path += "?" + part2;
        int port = getPort();
        String host = uri.getHost() + (port != WebSocket.DEFAULT_PORT ? ":" + port : "");
        HandshakeImpl1Client handshake = new HandshakeImpl1Client();
        handshake.setResourceDescriptor(path);
        handshake.put("Host", host);
        if (headers != null) {
            for (Map.Entry<String, String> kv : headers.entrySet()) {
                handshake.put(kv.getKey(), kv.getValue());
            }
        }
        engine.startHandshake(handshake);
    }

    public READYSTATE getReadyState() {
        return engine.getReadyState();
    }

    @Override
    public final void onWebsocketMessage(WebSocket conn, String message) {
        onMessage(message);
    }

    @Override
    public final void onWebsocketMessage(WebSocket conn, ByteBuffer blob) {
        onMessage(blob);
    }

    @Override
    public void onWebsocketMessageFragment(WebSocket conn, Framedata frame) {
        onFragment(frame);
    }

    @Override
    public final void onWebsocketOpen(WebSocket conn, Handshakedata handshake) {
        connectLatch.countDown();
        onOpen((ServerHandshake) handshake);
    }

    @Override
    public final void onWebsocketClose(WebSocket conn, int code, String reason, boolean remote) {
        connectLatch.countDown();
        closeLatch.countDown();
        if (writeThread != null)
            writeThread.interrupt();
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            onWebsocketError(this, e);
        }
        onClose(code, reason, remote);
    }

    /**
     * Calls subclass' implementation of <var>onIOError</var>.
     */
    @Override
    public final void onWebsocketError(WebSocket conn, Exception ex) {
        onError(ex);
    }

    @Override
    public final void onWriteDemand(WebSocket conn) {
        // nothing to do
    }

    @Override
    public void onWebsocketCloseInitiated(WebSocket conn, int code, String reason) {
        onCloseInitiated(code, reason);
    }

    @Override
    public void onWebsocketClosing(WebSocket conn, int code, String reason, boolean remote) {
        onClosing(code, reason, remote);
    }

    public void onCloseInitiated(int code, String reason) {
    }

    public void onClosing(int code, String reason, boolean remote) {
    }

    public WebSocket getConnection() {
        return engine;
    }

    @Override
    public InetSocketAddress getLocalSocketAddress(WebSocket conn) {
        if (socket != null)
            return (InetSocketAddress) socket.getLocalSocketAddress();
        return null;
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress(WebSocket conn) {
        if (socket != null)
            return (InetSocketAddress) socket.getRemoteSocketAddress();
        return null;
    }

    public abstract void onOpen(ServerHandshake handshakedata);

    public abstract void onMessage(String message);

    public abstract void onClose(int code, String reason, boolean remote);

    public abstract void onError(Exception ex);

    public void onMessage(ByteBuffer bytes) {
    }

    public void onFragment(Framedata frame) {
    }

    private class WebsocketWriteThread implements Runnable {
        @Override
        public void run() {
            Thread.currentThread().setName("WebsocketWriteThread");
            try {
                while (!Thread.interrupted()) {
                    ByteBuffer buffer = engine.outQueue.take();
                    ostream.write(buffer.array(), 0, buffer.limit());
                    ostream.flush();
                }
            } catch (IOException e) {
                engine.eot();
            } catch (InterruptedException e) {
                // this thread is regularly terminated via an interrupt
            }
        }
    }

    public void setProxy(Proxy proxy) {
        if (proxy == null)
            throw new IllegalArgumentException();
        this.proxy = proxy;
    }

    public void setSocket(Socket socket) {
        if (this.socket != null) {
            throw new IllegalStateException("socket has already been set");
        }
        this.socket = socket;
    }

    @Override
    public void sendFragmentedFrame(Opcode op, ByteBuffer buffer, boolean fin) {
        engine.sendFragmentedFrame(op, buffer, fin);
    }

    @Override
    public boolean isOpen() {
        return engine.isOpen();
    }

    @Override
    public boolean isFlushAndClose() {
        return engine.isFlushAndClose();
    }

    @Override
    public boolean isClosed() {
        return engine.isClosed();
    }

    @Override
    public boolean isClosing() {
        return engine.isClosing();
    }

    @Override
    public boolean isConnecting() {
        return engine.isConnecting();
    }

    @Override
    public boolean hasBufferedData() {
        return engine.hasBufferedData();
    }

    @Override
    public void close(int code) {
        engine.close();
    }

    @Override
    public void close(int code, String message) {
        engine.close(code, message);
    }

    @Override
    public void closeConnection(int code, String message) {
        engine.closeConnection(code, message);
    }

    @Override
    public void send(ByteBuffer bytes) throws IllegalArgumentException, NotYetConnectedException {
        engine.send(bytes);
    }

    @Override
    public void sendFrame(Framedata framedata) {
        engine.sendFrame(framedata);
    }

    @Override
    public InetSocketAddress getLocalSocketAddress() {
        return engine.getLocalSocketAddress();
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress() {
        return engine.getRemoteSocketAddress();
    }

    @Override
    public String getResourceDescriptor() {
        return uri.getPath();
    }
}
