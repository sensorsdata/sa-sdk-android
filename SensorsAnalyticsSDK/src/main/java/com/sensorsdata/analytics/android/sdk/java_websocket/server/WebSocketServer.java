package com.sensorsdata.analytics.android.sdk.java_websocket.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sensorsdata.analytics.android.sdk.java_websocket.SocketChannelIOHelper;
import com.sensorsdata.analytics.android.sdk.java_websocket.WebSocket;
import com.sensorsdata.analytics.android.sdk.java_websocket.WebSocketAdapter;
import com.sensorsdata.analytics.android.sdk.java_websocket.WebSocketFactory;
import com.sensorsdata.analytics.android.sdk.java_websocket.WebSocketImpl;
import com.sensorsdata.analytics.android.sdk.java_websocket.WrappedByteChannel;
import com.sensorsdata.analytics.android.sdk.java_websocket.drafts.Draft;
import com.sensorsdata.analytics.android.sdk.java_websocket.exceptions.InvalidDataException;
import com.sensorsdata.analytics.android.sdk.java_websocket.framing.CloseFrame;
import com.sensorsdata.analytics.android.sdk.java_websocket.framing.Framedata;
import com.sensorsdata.analytics.android.sdk.java_websocket.handshake.ClientHandshake;
import com.sensorsdata.analytics.android.sdk.java_websocket.handshake.Handshakedata;
import com.sensorsdata.analytics.android.sdk.java_websocket.handshake.ServerHandshakeBuilder;

/**
 * <tt>WebSocketServer</tt> is an abstract class that only takes care of the
 * HTTP handshake portion of WebSockets. It's up to a subclass to add
 * functionality/purpose to the server.
 */
public abstract class WebSocketServer extends WebSocketAdapter implements Runnable {

    public static int DECODERS = Runtime.getRuntime().availableProcessors();

    private final Collection<WebSocket> connections;

    private final InetSocketAddress address;
    /**
     * The socket channel for this WebSocket server.
     */
    private ServerSocketChannel server;
    /**
     * The 'Selector' used to get event keys from the underlying socket.
     */
    private Selector selector;
    /**
     * The Draft of the WebSocket protocol the Server is adhering to.
     */
    private List<Draft> drafts;

    private Thread selectorthread;

    private final AtomicBoolean isclosed = new AtomicBoolean(false);

    private List<WebSocketWorker> decoders;

    private List<WebSocketImpl> iqueue;
    private BlockingQueue<ByteBuffer> buffers;
    private int queueinvokes = 0;
    private final AtomicInteger queuesize = new AtomicInteger(0);

    private WebSocketServerFactory wsf = new DefaultWebSocketServerFactory();

    public WebSocketServer() throws UnknownHostException {
        this(new InetSocketAddress(WebSocket.DEFAULT_PORT), DECODERS, null);
    }

    public WebSocketServer(InetSocketAddress address) {
        this(address, DECODERS, null);
    }

    public WebSocketServer(InetSocketAddress address, int decoders) {
        this(address, decoders, null);
    }

    public WebSocketServer(InetSocketAddress address, List<Draft> drafts) {
        this(address, DECODERS, drafts);
    }

    public WebSocketServer(InetSocketAddress address, int decodercount, List<Draft> drafts) {
        this(address, decodercount, drafts, new HashSet<WebSocket>());
    }

    public WebSocketServer(InetSocketAddress address, int decodercount, List<Draft> drafts, Collection<WebSocket> connectionscontainer) {
        if (address == null || decodercount < 1 || connectionscontainer == null) {
            throw new IllegalArgumentException("address and connectionscontainer must not be null and you need at least 1 decoder");
        }

        if (drafts == null)
            this.drafts = Collections.emptyList();
        else
            this.drafts = drafts;

        this.address = address;
        this.connections = connectionscontainer;

        iqueue = new LinkedList<WebSocketImpl>();

        decoders = new ArrayList<WebSocketWorker>(decodercount);
        buffers = new LinkedBlockingQueue<ByteBuffer>();
        for (int i = 0; i < decodercount; i++) {
            WebSocketWorker ex = new WebSocketWorker();
            decoders.add(ex);
            ex.start();
        }
    }

    public void start() {
        if (selectorthread != null)
            throw new IllegalStateException(getClass().getName() + " can only be started once.");
        new Thread(this).start();
    }

    public void stop(int timeout) throws InterruptedException {
        if (!isclosed.compareAndSet(false, true)) { // this also makes sure that no further connections will be added to this.connections
            return;
        }

        List<WebSocket> socketsToClose = null;

        // copy the connections in a list (prevent callback deadlocks)
        synchronized (connections) {
            socketsToClose = new ArrayList<WebSocket>(connections);
        }

        for (WebSocket ws : socketsToClose) {
            ws.close(CloseFrame.GOING_AWAY);
        }

        synchronized (this) {
            if (selectorthread != null && selectorthread != Thread.currentThread()) {
                selector.wakeup();
                selectorthread.interrupt();
                selectorthread.join(timeout);
            }
        }
    }

    public void stop() throws IOException, InterruptedException {
        stop(0);
    }

    public Collection<WebSocket> connections() {
        return this.connections;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public int getPort() {
        int port = getAddress().getPort();
        if (port == 0 && server != null) {
            port = server.socket().getLocalPort();
        }
        return port;
    }

    public List<Draft> getDraft() {
        return Collections.unmodifiableList(drafts);
    }

    public void run() {
        synchronized (this) {
            if (selectorthread != null)
                throw new IllegalStateException(getClass().getName() + " can only be started once.");
            selectorthread = Thread.currentThread();
            if (isclosed.get()) {
                return;
            }
        }
        selectorthread.setName("WebsocketSelector" + selectorthread.getId());
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            ServerSocket socket = server.socket();
            socket.setReceiveBufferSize(WebSocketImpl.RCVBUF);
            socket.bind(address);
            selector = Selector.open();
            server.register(selector, server.validOps());
        } catch (IOException ex) {
            handleFatal(null, ex);
            return;
        }
        try {
            while (!selectorthread.isInterrupted()) {
                SelectionKey key = null;
                WebSocketImpl conn = null;
                try {
                    selector.select();
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> i = keys.iterator();

                    while (i.hasNext()) {
                        key = i.next();

                        if (!key.isValid()) {
                            // Object o = key.attachment();
                            continue;
                        }

                        if (key.isAcceptable()) {
                            if (!onConnect(key)) {
                                key.cancel();
                                continue;
                            }

                            SocketChannel channel = server.accept();
                            channel.configureBlocking(false);
                            WebSocketImpl w = wsf.createWebSocket(this, drafts, channel.socket());
                            w.key = channel.register(selector, SelectionKey.OP_READ, w);
                            w.channel = wsf.wrapChannel(channel, w.key);
                            i.remove();
                            allocateBuffers(w);
                            continue;
                        }

                        if (key.isReadable()) {
                            conn = (WebSocketImpl) key.attachment();
                            ByteBuffer buf = takeBuffer();
                            try {
                                if (SocketChannelIOHelper.read(buf, conn, conn.channel)) {
                                    if (buf.hasRemaining()) {
                                        conn.inQueue.put(buf);
                                        queue(conn);
                                        i.remove();
                                        if (conn.channel instanceof WrappedByteChannel) {
                                            if (((WrappedByteChannel) conn.channel).isNeedRead()) {
                                                iqueue.add(conn);
                                            }
                                        }
                                    } else
                                        pushBuffer(buf);
                                } else {
                                    pushBuffer(buf);
                                }
                            } catch (IOException e) {
                                pushBuffer(buf);
                                throw e;
                            }
                        }
                        if (key.isWritable()) {
                            conn = (WebSocketImpl) key.attachment();
                            if (SocketChannelIOHelper.batch(conn, conn.channel)) {
                                if (key.isValid())
                                    key.interestOps(SelectionKey.OP_READ);
                            }
                        }
                    }
                    while (!iqueue.isEmpty()) {
                        conn = iqueue.remove(0);
                        WrappedByteChannel c = ((WrappedByteChannel) conn.channel);
                        ByteBuffer buf = takeBuffer();
                        try {
                            if (SocketChannelIOHelper.readMore(buf, conn, c))
                                iqueue.add(conn);
                            if (buf.hasRemaining()) {
                                conn.inQueue.put(buf);
                                queue(conn);
                            } else {
                                pushBuffer(buf);
                            }
                        } catch (IOException e) {
                            pushBuffer(buf);
                            throw e;
                        }

                    }
                } catch (CancelledKeyException e) {
                    // an other thread may cancel the key
                } catch (ClosedByInterruptException e) {
                    return; // do the same stuff as when InterruptedException is thrown
                } catch (IOException ex) {
                    if (key != null)
                        key.cancel();
                    handleIOException(key, conn, ex);
                } catch (InterruptedException e) {
                    return;// FIXME controlled shutdown (e.g. take care of buffermanagement)
                }
            }

        } catch (RuntimeException e) {
            // should hopefully never occur
            handleFatal(null, e);
        } finally {
            if (decoders != null) {
                for (WebSocketWorker w : decoders) {
                    w.interrupt();
                }
            }
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    onError(null, e);
                }
            }
        }
    }

    protected void allocateBuffers(WebSocket c) throws InterruptedException {
        if (queuesize.get() >= 2 * decoders.size() + 1) {
            return;
        }
        queuesize.incrementAndGet();
        buffers.put(createBuffer());
    }

    protected void releaseBuffers(WebSocket c) throws InterruptedException {
        // queuesize.decrementAndGet();
        // takeBuffer();
    }

    public ByteBuffer createBuffer() {
        return ByteBuffer.allocate(WebSocketImpl.RCVBUF);
    }

    private void queue(WebSocketImpl ws) throws InterruptedException {
        if (ws.workerThread == null) {
            ws.workerThread = decoders.get(queueinvokes % decoders.size());
            queueinvokes++;
        }
        ws.workerThread.put(ws);
    }

    private ByteBuffer takeBuffer() throws InterruptedException {
        return buffers.take();
    }

    private void pushBuffer(ByteBuffer buf) throws InterruptedException {
        if (buffers.size() > queuesize.intValue())
            return;
        buffers.put(buf);
    }

    private void handleIOException(SelectionKey key, WebSocket conn, IOException ex) {
        // onWebsocketError( conn, ex );// conn may be null here
        if (conn != null) {
            conn.closeConnection(CloseFrame.ABNORMAL_CLOSE, ex.getMessage());
        } else if (key != null) {
            SelectableChannel channel = key.channel();
            if (channel != null && channel.isOpen()) { // this could be the case if the IOException ex is a SSLException
                try {
                    channel.close();
                } catch (IOException e) {
                    // there is nothing that must be done here
                }
            }
        }
    }

    private void handleFatal(WebSocket conn, Exception e) {
        onError(conn, e);
        try {
            stop();
        } catch (IOException e1) {
            onError(null, e1);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
            onError(null, e1);
        }
    }

    protected String getFlashSecurityPolicy() {
        return "<cross-domain-policy><allow-access-from domain=\"*\" to-ports=\"" + getPort() + "\" /></cross-domain-policy>";
    }

    @Override
    public final void onWebsocketMessage(WebSocket conn, String message) {
        onMessage(conn, message);
    }

    @Override
    @Deprecated
    public/*final*/void onWebsocketMessageFragment(WebSocket conn, Framedata frame) {// onFragment should be overloaded instead
        onFragment(conn, frame);
    }

    @Override
    public final void onWebsocketMessage(WebSocket conn, ByteBuffer blob) {
        onMessage(conn, blob);
    }

    @Override
    public final void onWebsocketOpen(WebSocket conn, Handshakedata handshake) {
        if (addConnection(conn)) {
            onOpen(conn, (ClientHandshake) handshake);
        }
    }

    @Override
    public final void onWebsocketClose(WebSocket conn, int code, String reason, boolean remote) {
        selector.wakeup();
        try {
            if (removeConnection(conn)) {
                onClose(conn, code, reason, remote);
            }
        } finally {
            try {
                releaseBuffers(conn);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    protected boolean removeConnection(WebSocket ws) {
        boolean removed;
        synchronized (connections) {
            removed = this.connections.remove(ws);
            assert (removed);
        }
        if (isclosed.get() && connections.size() == 0) {
            selectorthread.interrupt();
        }
        return removed;
    }

    @Override
    public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
        return super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
    }

    protected boolean addConnection(WebSocket ws) {
        if (!isclosed.get()) {
            synchronized (connections) {
                boolean succ = this.connections.add(ws);
                assert (succ);
                return succ;
            }
        } else {
            // This case will happen when a new connection gets ready while the server is already stopping.
            ws.close(CloseFrame.GOING_AWAY);
            return true;// for consistency sake we will make sure that both onOpen will be called
        }
    }

    @Override
    public final void onWebsocketError(WebSocket conn, Exception ex) {
        onError(conn, ex);
    }

    @Override
    public final void onWriteDemand(WebSocket w) {
        WebSocketImpl conn = (WebSocketImpl) w;
        try {
            conn.key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        } catch (CancelledKeyException e) {
            // the thread which cancels key is responsible for possible cleanup
            conn.outQueue.clear();
        }
        selector.wakeup();
    }

    @Override
    public void onWebsocketCloseInitiated(WebSocket conn, int code, String reason) {
        onCloseInitiated(conn, code, reason);
    }

    @Override
    public void onWebsocketClosing(WebSocket conn, int code, String reason, boolean remote) {
        onClosing(conn, code, reason, remote);

    }

    public void onCloseInitiated(WebSocket conn, int code, String reason) {
    }

    public void onClosing(WebSocket conn, int code, String reason, boolean remote) {

    }

    public final void setWebSocketFactory(WebSocketServerFactory wsf) {
        this.wsf = wsf;
    }

    public final WebSocketFactory getWebSocketFactory() {
        return wsf;
    }

    protected boolean onConnect(SelectionKey key) {
        return true;
    }

    private Socket getSocket(WebSocket conn) {
        WebSocketImpl impl = (WebSocketImpl) conn;
        return ((SocketChannel) impl.key.channel()).socket();
    }

    @Override
    public InetSocketAddress getLocalSocketAddress(WebSocket conn) {
        return (InetSocketAddress) getSocket(conn).getLocalSocketAddress();
    }

    @Override
    public InetSocketAddress getRemoteSocketAddress(WebSocket conn) {
        return (InetSocketAddress) getSocket(conn).getRemoteSocketAddress();
    }

    public abstract void onOpen(WebSocket conn, ClientHandshake handshake);

    public abstract void onClose(WebSocket conn, int code, String reason, boolean remote);

    public abstract void onMessage(WebSocket conn, String message);

    public abstract void onError(WebSocket conn, Exception ex);

    public void onMessage(WebSocket conn, ByteBuffer message) {
    }

    public void onFragment(WebSocket conn, Framedata fragment) {
    }

    public class WebSocketWorker extends Thread {

        private BlockingQueue<WebSocketImpl> iqueue;

        public WebSocketWorker() {
            iqueue = new LinkedBlockingQueue<WebSocketImpl>();
            setName("WebSocketWorker-" + getId());
            setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    getDefaultUncaughtExceptionHandler().uncaughtException(t, e);
                }
            });
        }

        public void put(WebSocketImpl ws) throws InterruptedException {
            iqueue.put(ws);
        }

        @Override
        public void run() {
            WebSocketImpl ws = null;
            try {
                while (true) {
                    ByteBuffer buf = null;
                    ws = iqueue.take();
                    buf = ws.inQueue.poll();
                    assert (buf != null);
                    try {
                        ws.decode(buf);
                    } catch (Exception e) {
                        System.err.println("Error while reading from remote connection: " + e);
                    } finally {
                        pushBuffer(buf);
                    }
                }
            } catch (InterruptedException e) {
            } catch (RuntimeException e) {
                handleFatal(ws, e);
            }
        }
    }

    public interface WebSocketServerFactory extends WebSocketFactory {
        @Override
        public WebSocketImpl createWebSocket(WebSocketAdapter a, Draft d, Socket s);

        public WebSocketImpl createWebSocket(WebSocketAdapter a, List<Draft> drafts, Socket s);

        public ByteChannel wrapChannel(SocketChannel channel, SelectionKey key) throws IOException;
    }
}
