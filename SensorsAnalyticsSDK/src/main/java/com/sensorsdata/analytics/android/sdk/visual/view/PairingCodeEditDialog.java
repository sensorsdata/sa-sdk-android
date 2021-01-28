/*
 * Created by zhangxiangwei on 2020/07/09.
 * Copyright 2015－2020 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.visual.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.sensorsdata.analytics.android.sdk.R;
import com.sensorsdata.analytics.android.sdk.SALog;

public class PairingCodeEditDialog extends Dialog {

    private static final String TAG = "SA.PairingCodeEditDialog";
    private Context mContext;

    public PairingCodeEditDialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensors_analytics_verification_code);
        Window window = getWindow();
        if (window != null) {
            WindowManager.LayoutParams p = window.getAttributes();
            p.width = dip2px(getContext(), 350);
            window.setAttributes(p);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dip2px(getContext(), 7));
            window.setBackgroundDrawable(bg);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        final PairingCodeEditText pairingCodeEditText = findViewById(R.id.sensors_analytics_pairing_code);
        pairingCodeEditText.setOnPairingCodeChangedListener(new IPairingCodeInterface.OnPairingCodeChangedListener() {
            @Override
            public void onPairingCodeChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void onInputCompleted(CharSequence s) {
                if (TextUtils.isEmpty(s)) {
                    SALog.i(TAG, "onCreate | dialog input content is null and return");
                    return;
                }
                new PairingCodeRequestHelper().verifyPairingCodeRequest(mContext, s.toString(), new PairingCodeRequestHelper.IApiCallback() {
                    @Override
                    public void onSuccess() {
                        pairingCodeEditText.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                pairingCodeEditText.hiddenKeyBord();
                                PairingCodeEditDialog.this.dismiss();
                            }
                        }, 300);
                    }

                    @Override
                    public void onFailure(String message) {
                        pairingCodeEditText.clearText();
                        if (!TextUtils.isEmpty(message)) {
                            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void show() {
        if (!this.isLiving()) {
            SALog.i(TAG, "Activity is finish");
        } else {
            SALog.i(TAG, "show:" + this.mContext);
            super.show();
        }
    }

    @Override
    public void dismiss() {
        if (Looper.myLooper() != Looper.getMainLooper() && this.mContext instanceof Activity && isActivityFinishingOrDestroyed(this.mContext)) {
            SALog.i(TAG, "Activity is finish");
        } else {
            if (this.isShowing()) {
                try {
                    SALog.i(TAG, "isShowing() == true, dismiss");
                    super.dismiss();
                } catch (IllegalArgumentException e) {
                    SALog.printStackTrace(e);
                }
            }
        }
    }

    private boolean isLiving() {
        if (this.mContext == null) {
            return false;
        } else {
            if (this.mContext instanceof Activity && isActivityFinishingOrDestroyed(mContext)) {
                return false;
            }
        }
        return true;
    }

    private boolean isActivityFinishingOrDestroyed(Context context) {
        Activity activity = (Activity) context;
        if (activity.isFinishing()) {
            SALog.i(TAG, "Activity is finish,name=" + activity.getClass().getName());
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return activity.isDestroyed();
        }
        return false;
    }

    private int dip2px(Context context, float dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
