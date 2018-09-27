/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

import android.annotation.TargetApi;
import android.view.View;

import java.util.List;

@TargetApi(SensorsDataAPI.VTRACK_SUPPORTED_MIN_API)
public abstract class ViewVisitor implements Pathfinder.Accumulator {

    private final List<Pathfinder.PathElement> mPath;
    private final Pathfinder mPathfinder;

    private static final String TAG = "SA.ViewVisitor";

    public void visit(View rootView) {
        mPathfinder.findTargetsInRoot(rootView, mPath, this);
    }

    /**
     * 清除所有事件监听，调用后ViewVisitor将失效
     */
    public abstract void cleanup();

    protected ViewVisitor(List<Pathfinder.PathElement> path) {
        mPath = path;
        mPathfinder = new Pathfinder();
    }

    protected abstract String name();
}
