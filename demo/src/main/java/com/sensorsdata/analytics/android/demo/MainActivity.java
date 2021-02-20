/*
 * Created by wangzhuohou on 2019/04/17.
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

package com.sensorsdata.analytics.android.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.databinding.DataBindingUtil;

import com.sensorsdata.analytics.android.demo.databinding.ActivityMainBinding;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setHandlers(this);

        initLambdaButton();
        initButton();
    }

    public void onViewClick(View view) {

    }

    private void initLambdaButton() {
        Button button = (Button) findViewById(R.id.lambdaButton);
        button.setOnClickListener(v -> {

        });
    }

    private void initButton() {
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
}
