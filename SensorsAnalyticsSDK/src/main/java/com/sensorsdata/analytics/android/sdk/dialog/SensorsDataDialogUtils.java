/*
 * Created by chenru on 2020/09/09.
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

package com.sensorsdata.analytics.android.sdk.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.TextUtils;

public class SensorsDataDialogUtils {

    public static AlertDialog showDialog(Activity activity, String title, String content,
                                         final String positiveLabel, final DialogInterface.OnClickListener positiveOnClickListener,
                                         final String negativeLabel, final DialogInterface.OnClickListener negativeOnClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }

        if (!TextUtils.isEmpty(content)) {
            builder.setMessage(content);
        }

        builder.setCancelable(false);
        builder.setNegativeButton(negativeLabel, negativeOnClickListener);
        builder.setPositiveButton(positiveLabel, positiveOnClickListener);
        return builder.create();
    }
}
