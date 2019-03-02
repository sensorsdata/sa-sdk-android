package com.sensorsdata.analytics.android.sdk;

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

class DebugModeSelectDialog extends Dialog implements View.OnClickListener {

    private OnDebugModeViewClickListener onDebugModeDialogClickListener;
    private SensorsDataAPI.DebugMode currentDebugMode;

    DebugModeSelectDialog(Context context, SensorsDataAPI.DebugMode debugMode) {
        super(context);
        currentDebugMode = debugMode;
    }

    interface OnDebugModeViewClickListener {
        void onCancel(Dialog dialog);
        void setDebugMode(Dialog dialog, SensorsDataAPI.DebugMode debugMode);
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
            p.width = dip2px(getContext(), 270);
            p.height = dip2px(getContext(), 240);
            window.setAttributes(p);
            //设置弹框圆角
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dip2px(getContext(),7));
            window.setBackgroundDrawable(bg);
        }
    }

    private void initView() {
        //标题:SDK 调试模式选择
        TextView debugModeTitle = (TextView) findViewById(R.id.sensors_analytics_debug_mode_title);
        debugModeTitle.setText("SDK 调试模式选择");

        //取消
        TextView debugModeCancel = (TextView) findViewById(R.id.sensors_analytics_debug_mode_cancel);
        debugModeCancel.setText("取消");
        debugModeCancel.setOnClickListener(this);

        //开启调试模式(不导入数据)
        TextView debugModeOnly = (TextView) findViewById(R.id.sensors_analytics_debug_mode_only);
        debugModeOnly.setText("开启调试模式（不导入数据）");
        debugModeOnly.setOnClickListener(this);

        //"开启调试模式(导入数据)"
        TextView debugModeTrack = (TextView) findViewById(R.id.sensors_analytics_debug_mode_track);
        debugModeTrack.setText("开启调试模式（导入数据）");
        debugModeTrack.setOnClickListener(this);

        String msg = "调试模式已关闭";
        if (currentDebugMode == SensorsDataAPI.DebugMode.DEBUG_ONLY) {
            msg = "当前为 调试模式（不导入数据）";
        } else if (currentDebugMode == SensorsDataAPI.DebugMode.DEBUG_AND_TRACK) {
            msg = "当前为 测试模式（导入数据）";
        }
        TextView debugModeMessage = (TextView) findViewById(R.id.sensors_analytics_debug_mode_message);
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
        stateListDrawable.addState(new int[]{}, normalDrawable);
        return stateListDrawable;
    }

    private int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
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
}
