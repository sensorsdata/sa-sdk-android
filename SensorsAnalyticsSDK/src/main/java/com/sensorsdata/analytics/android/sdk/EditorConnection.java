package com.sensorsdata.analytics.android.sdk;

import com.sensorsdata.analytics.android.sdk.java_websocket.client.WebSocketClient;
import com.sensorsdata.analytics.android.sdk.java_websocket.drafts.Draft_17;
import com.sensorsdata.analytics.android.sdk.java_websocket.exceptions.NotSendableException;
import com.sensorsdata.analytics.android.sdk.java_websocket.exceptions.WebsocketNotConnectedException;
import com.sensorsdata.analytics.android.sdk.java_websocket.framing.Framedata;
import com.sensorsdata.analytics.android.sdk.java_websocket.handshake.ServerHandshake;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * EditorClient should handle all communication to and from the socket. It should be fairly naive and
 * only know how to delegate messages to the ABHandler class.
 */
public class EditorConnection {

    public class EditorConnectionException extends IOException {
        private static final long serialVersionUID = -1884953175346045636L;

        public EditorConnectionException(Throwable cause) {
            super(cause.getMessage()); // IOException(cause) is only available in API level 9!
        }
    }

    public interface Editor {
        void sendSnapshot(JSONObject message);

        void bindEvents(JSONObject message);

        void sendDeviceInfo(JSONObject message);

        void cleanup();

        void disconnect();

        void onWebSocketOpen();

        void onWebSocketClose(int code);
    }

    public EditorConnection(URI uri, Editor service)
            throws EditorConnectionException {
        mService = service;
        mURI = uri;
        try {
            mClient = new EditorClient(uri, CONNECT_TIMEOUT);
            mClient.connectBlocking();
        } catch (final InterruptedException e) {
            throw new EditorConnectionException(e);
        }
    }

    public boolean isValid() {
        return !mClient.isClosed() && !mClient.isClosing() && !mClient.isFlushAndClose();
    }

    public BufferedOutputStream getBufferedOutputStream() {
        return new BufferedOutputStream(new WebSocketOutputStream());
    }

    public void sendMessage(String message) {
        SALog.i(TAG, "Sending message: " + message);
        try {
            mClient.send(message);
        } catch (Exception e) {
            SALog.i(TAG, "sendMessage;error", e);
        }
    }

    public void close(boolean block) {
        if (mClient == null) {
            return;
        }
        try {
            if (block) {
                mClient.closeBlocking();
            } else {
                mClient.close();
            }
        } catch (Exception e) {
            SALog.i(TAG, "close;error", e);
        }
    }

    private class EditorClient extends WebSocketClient {

        public EditorClient(URI uri, int connectTimeout) throws InterruptedException {
            super(uri, new Draft_17(), null, connectTimeout);
        }

        @Override
        public void onOpen(ServerHandshake handShakeData) {
            if (SensorsDataAPI.ENABLE_LOG) {
                SALog.i(TAG, "WebSocket connected: " + handShakeData.getHttpStatus() + " " + handShakeData
                        .getHttpStatusMessage());
            }

            mService.onWebSocketOpen();
        }

        @Override
        public void onMessage(String message) {
//      Log.i(LOGTAG, "Received message from editor:\n" + message);

            try {
                final JSONObject messageJson = new JSONObject(message);
                final String type = messageJson.getString("type");
                if (type.equals("device_info_request")) {
                    mService.sendDeviceInfo(messageJson);
                } else if (type.equals("snapshot_request")) {
                    mService.sendSnapshot(messageJson);
                } else if (type.equals("event_binding_request")) {
                    mService.bindEvents(messageJson);
                } else if (type.equals("disconnect")) {
                    mService.disconnect();
                }
            } catch (final JSONException e) {
                SALog.i(TAG, "Bad JSON received:" + message, e);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.i(TAG, "WebSocket closed. Code: " + code + ", reason: " + reason + "\nURI: " + mURI);
            mService.cleanup();
            mService.onWebSocketClose(code);
        }

        @Override
        public void onError(Exception ex) {
            if (ex != null && ex.getMessage() != null) {
                SALog.i(TAG, "Websocket Error: " + ex.getMessage());
            } else {
                SALog.i(TAG, "Unknown websocket error occurred");
            }
        }
    }


    private class WebSocketOutputStream extends OutputStream {
        @Override
        public void write(int b)
                throws EditorConnectionException {
            // This should never be called.
            final byte[] oneByte = new byte[1];
            oneByte[0] = (byte) b;
            write(oneByte, 0, 1);
        }

        @Override
        public void write(byte[] b)
                throws EditorConnectionException {
            write(b, 0, b.length);
        }

        @Override
        public void write(byte[] b, int off, int len)
                throws EditorConnectionException {
            final ByteBuffer message = ByteBuffer.wrap(b, off, len);
            try {
                mClient.sendFragmentedFrame(Framedata.Opcode.TEXT, message, false);
            } catch (final WebsocketNotConnectedException e) {
                throw new EditorConnectionException(e);
            } catch (final NotSendableException e) {
                throw new EditorConnectionException(e);
            }
        }

        @Override
        public void close()
                throws EditorConnectionException {
            try {
                mClient.sendFragmentedFrame(Framedata.Opcode.TEXT, EMPTY_BYTE_BUFFER, true);
            } catch (final WebsocketNotConnectedException e) {
                throw new EditorConnectionException(e);
            } catch (final NotSendableException e) {
                throw new EditorConnectionException(e);
            }
        }
    }

    private final Editor mService;
    private final EditorClient mClient;
    private final URI mURI;

    private static final int CONNECT_TIMEOUT = 1000;
    private static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocate(0);

    private static final String TAG = "SA.EditorConnection";
}
