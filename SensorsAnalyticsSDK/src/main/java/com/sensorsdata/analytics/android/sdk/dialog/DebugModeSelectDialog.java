/*
 * Created by yuejianzhong on 2020/11/04.
 * Copyright 2015－2021 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;

class DebugModeSelectDialog extends Dialog implements View.OnClickListener {

    private OnDebugModeViewClickListener onDebugModeDialogClickListener;
    private SensorsDataAPI.DebugMode currentDebugMode;

    DebugModeSelectDialog(Context context, SensorsDataAPI.DebugMode debugMode) {
        super(context);
        currentDebugMode = debugMode;
    }

    void setOnDebugModeDialogClickListener(OnDebugModeViewClickListener onDebugModeDialogClickListener) {
        this.onDebugModeDialogClickListener = onDebugModeDialogClickListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.sensors_analytics_debug_mode_dialog_content);
        initView();
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams p = window.getAttributes();
            p.width = SADisplayUtil.dip2px(getContext(), 270);
            p.height = SADisplayUtil.dip2px(getContext(), 240);
            window.setAttributes(p);
            //设置弹框圆角
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(SADisplayUtil.dip2px(getContext(), 7));
            window.setBackgroundDrawable(bg);
        }
    }

    private void initView() {
        //标题:SDK 调试模式选择
        TextView debugModeTitle = findViewById(R.id.sensors_analytics_debug_mode_title);
        debugModeTitle.setText("SDK 调试模式选择");
        //取消
        TextView debugModeCancel = findViewById(R.id.sensors_analytics_debug_mode_cancel);
        debugModeCancel.setText("取消");
        debugModeCancel.setOnClickListener(this);
        debugModeCancel.setFocusable(true);
        //开启调试模式(不导入数据)
        TextView debugModeOnly = findViewById(R.id.sensors_analytics_debug_mode_only);
        debugModeOnly.setText("开启调试模式（不导入数据）");
        debugModeOnly.setOnClickListener(this);
        debugModeOnly.setFocusable(true);
        //"开启调试模式(导入数据)"
        TextView debugModeTrack = findViewById(R.id.sensors_analytics_debug_mode_track);
        debugModeTrack.setText("开启调试模式（导入数据）");
        debugModeTrack.setOnClickListener(this);
        debugModeTrack.setFocusable(true);

        String msg = "调试模式已关闭";
        if (currentDebugMode == SensorsDataAPI.DebugMode.DEBUG_ONLY) {
            msg = "当前为 调试模式（不导入数据）";
        } else if (currentDebugMode == SensorsDataAPI.DebugMode.DEBUG_AND_TRACK) {
            msg = "当前为 测试模式（导入数据）";
        }
        TextView debugModeMessage = findViewById(R.id.sensors_analytics_debug_mode_message);
        debugModeMessage.setText(msg);

        //设置按钮点击效果
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            debugModeCancel.setBackground(getDrawable());
            debugModeOnly.setBackground(getDrawable());
            debugModeTrack.setBackground(getDrawable());
        } else {
            debugModeCancel.setBackgroundDrawable(getDrawable());
            debugModeOnly.setBackgroundDrawable(getDrawable());
            debugModeTrack.setBackgroundDrawable(getDrawable());
        }
    }

    private StateListDrawable getDrawable() {
        GradientDrawable pressDrawable = new GradientDrawable();
        pressDrawable.setShape(GradientDrawable.RECTANGLE);
        pressDrawable.setColor(Color.parseColor("#dddddd"));

        GradientDrawable normalDrawable = new GradientDrawable();
        normalDrawable.setShape(GradientDrawable.RECTANGLE);
        normalDrawable.setColor(Color.WHITE);

        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressDrawable);
        stateListDrawable.addState(new int[]{android.R.attr.state_focused}, pressDrawable);
        stateListDrawable.addState(new int[]{}, normalDrawable);
        return stateListDrawable;
    }

    @Override
    public void onClick(View v) {
        if (onDebugModeDialogClickListener == null) {
            return;
        }
        int id = v.getId();
        if (id == R.id.sensors_analytics_debug_mode_track) {
            onDebugModeDialogClickListener.setDebugMode(this, SensorsDataAPI.DebugMode.DEBUG_AND_TRACK);
        } else if (id == R.id.sensors_analytics_debug_mode_only) {
            onDebugModeDialogClickListener.setDebugMode(this, SensorsDataAPI.DebugMode.DEBUG_ONLY);
        } else if (id == R.id.sensors_analytics_debug_mode_cancel) {
            onDebugModeDialogClickListener.onCancel(this);
        }
    }

    interface OnDebugModeViewClickListener {
        void onCancel(Dialog dialog);

        void setDebugMode(Dialog dialog, SensorsDataAPI.DebugMode debugMode);
    }
}
