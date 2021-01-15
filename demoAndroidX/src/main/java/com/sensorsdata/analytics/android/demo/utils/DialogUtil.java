/*
 * Created by chenru on 2019/06/20.
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

package com.sensorsdata.analytics.android.demo.utils;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;

import com.sensorsdata.analytics.android.demo.R;

public class DialogUtil {
    @SuppressWarnings("InflateParams")
    public static void showNeedReLoginDialog(final Context context) {
        try {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (inflater == null) {
                return;
            }
            View contentView = inflater.inflate(R.layout.dialog_modal, null);
            AppCompatTextView actionIKnow = contentView.findViewById(R.id.actionIKnow);
            AppCompatTextView errorMsgView = contentView.findViewById(R.id.errorMsg);
            AppCompatTextView errorTitleView = contentView.findViewById(R.id.errorTitle);
            errorMsgView.setText("登录失效，请重新登录");
            errorTitleView.setText("登录失败");

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(contentView);
            final Dialog dialog = builder.create();
            Window window = dialog.getWindow();
            if (window != null) {
                try {
                    window.setBackgroundDrawableResource(android.R.color.transparent);
                    window.getDecorView().setPadding(50, 0, 50, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            actionIKnow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.cancel();
                }
            });
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 登录失败提示框
     *
     * @param context Context
     * @param title String 标题
     * @param message String 提示语
     */
    @SuppressWarnings("InflateParams")
    public static void showModalDialog(Context context, String title, String message) {
        showModalDialog(context, title, message, null);
    }

    @SuppressWarnings("InflateParams")
    public static void showModalDialog(Context context, String title, String message, final View.OnClickListener listener) {
        try {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (inflater == null) {
                return;
            }
            View contentView = inflater.inflate(R.layout.dialog_modal, null);
            AppCompatTextView actionIKnow = contentView.findViewById(R.id.actionIKnow);
            AppCompatTextView errorMsgView = contentView.findViewById(R.id.errorMsg);
            AppCompatTextView errorTitleView = contentView.findViewById(R.id.errorTitle);
            errorMsgView.setText(message);
            errorTitleView.setText(title);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(contentView);
            final Dialog dialog = builder.create();
            Window window = dialog.getWindow();
            if (window != null) {
                try {
                    window.setBackgroundDrawableResource(android.R.color.transparent);
                    window.getDecorView().setPadding(50, 0, 50, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            actionIKnow.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) {
                    listener.onClick(v);
                }
            });
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
