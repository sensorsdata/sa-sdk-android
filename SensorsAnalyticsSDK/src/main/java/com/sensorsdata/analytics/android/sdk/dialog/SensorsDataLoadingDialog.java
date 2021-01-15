/*
 * Created by chenru on 2020/08/31.
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
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.util.SADisplayUtil;

public class SensorsDataLoadingDialog extends Dialog {

    private RelativeLayout mLoadingLayout;

    public SensorsDataLoadingDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.sensors_analytics_dialog_loading);
        Window window = getWindow();
        setCancelable(false);
        if (window != null) {
            WindowManager.LayoutParams p = window.getAttributes();
            p.height = SADisplayUtil.dip2px(getContext(), 98);
            p.width = SADisplayUtil.dip2px(getContext(), 88);
            window.setAttributes(p);
            //设置弹框圆角
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(SADisplayUtil.dip2px(getContext(), 7));
            window.setBackgroundDrawable(bg);
        }
        initView();

    }

    private void initView() {
        mLoadingLayout = findViewById(R.id.sensors_analytics_rotate_layout);
        ImageView imageView1 = findViewById(R.id.sensorsdata_analytics_loading_image1);
        ImageView imageView2 = findViewById(R.id.sensorsdata_analytics_loading_image2);
        ImageView imageView3 = findViewById(R.id.sensorsdata_analytics_loading_image3);
        ImageView imageView4 = findViewById(R.id.sensorsdata_analytics_loading_image4);
        setCircleBackground(imageView1, "#00C48E");
        setCircleBackground(imageView2, "#33D0A5");
        setCircleBackground(imageView3, "#CCF3E8");
        setCircleBackground(imageView4, "#80E1C6");
        initAnim();
    }

    private void setCircleBackground(View view, String color) {
        OvalShape ovalShape = new OvalShape();
        ShapeDrawable drawable = new ShapeDrawable(ovalShape);
        drawable.getPaint().setColor(Color.parseColor(color));
        drawable.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }

    private void initAnim() {
        Animation mAnimation = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mAnimation.setRepeatCount(-1);
        mAnimation.setDuration(1200);
        mAnimation.setInterpolator(new LinearInterpolator());
        mLoadingLayout.setAnimation(mAnimation);
    }
}
