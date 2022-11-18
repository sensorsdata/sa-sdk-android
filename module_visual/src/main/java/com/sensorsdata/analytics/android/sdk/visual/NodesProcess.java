package com.sensorsdata.analytics.android.sdk.visual;

public class NodesProcess {

    private volatile WebNodesManager mWebNodesManager;
    private volatile FlutterNodesManager mFlutterNodesManager;
    private volatile static NodesProcess mSingleton = null;

    public static NodesProcess getInstance() {
        if (mSingleton == null) {
            synchronized (NodesProcess.class) {
                if (mSingleton == null) {
                    mSingleton = new NodesProcess();
                }
            }
        }
        return mSingleton;
    }

    public WebNodesManager getWebNodesManager() {
        if (mWebNodesManager == null) {
            synchronized (WebNodesManager.class) {
                if (mWebNodesManager == null) {
                    mWebNodesManager = new WebNodesManager();
                }
            }
        }
        return mWebNodesManager;
    }

    public FlutterNodesManager getFlutterNodesManager() {
        if (mFlutterNodesManager == null) {
            synchronized (FlutterNodesManager.class) {
                if (mFlutterNodesManager == null) {
                    mFlutterNodesManager = new FlutterNodesManager();
                }
            }
        }
        return mFlutterNodesManager;
    }
}
