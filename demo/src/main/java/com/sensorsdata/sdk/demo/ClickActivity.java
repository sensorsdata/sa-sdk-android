/*
 * Created by dengshiwei on 2022/06/28.
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

package com.sensorsdata.sdk.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sensorsdata.analytics.android.sdk.SensorsDataIgnoreTrackAppClick;
import com.sensorsdata.sdk.demo.adapter.MyAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ClickActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_click);
        Button button = findViewById(R.id.button);
        TextView textView = findViewById(R.id.textView);
        EditText editText = findViewById(R.id.editText);
        CheckBox checkBox = findViewById(R.id.checkBox);
        RadioButton radioButton = findViewById(R.id.radioButton);
        Switch switchButton = findViewById(R.id.switchButton);
        ImageView imageView = findViewById(R.id.imageView);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        button.setOnClickListener(v -> textView.setText("Button Clicked"));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> textView.setText("CheckBox: " + isChecked));
        radioButton.setOnCheckedChangeListener((buttonView, isChecked) -> textView.setText("RadioButton: " + isChecked));
        switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> textView.setText("Switch: " + isChecked));
        imageView.setOnClickListener(v -> progressBar.setVisibility(View.VISIBLE));

    }


}
