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

package com.sensorsdata.analytics.android.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.databinding.ActivityClickBinding;
import com.sensorsdata.analytics.android.demo.entity.BindingEntity;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackViewOnClick;


public class ClickActivity extends BaseActivity implements View.OnClickListener {

    private ActivityClickBinding activityClickBinding = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_click);
        activityClickBinding = DataBindingUtil.setContentView(this, R.layout.activity_click);
        this.setTitle("设置点击方式");
        initView();
    }

    private void initView() {
        type1();
        type2();
        type3();
        type5();
        type8();
        type9();
    }

    /**
     * 1. 匿名内部类方式。
     */
    private void type1() {
        findViewById(R.id.tv_click_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ClickActivity.this, "方式1", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 2. 实现 View.OnClickListener 接口重写 onClick 方式。
     */
    private void type2() {
        findViewById(R.id.tv_click_2).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Toast.makeText(ClickActivity.this, "方式2", Toast.LENGTH_SHORT).show();
    }


    /**
     * 3. View.OnClickListener 的实现类方式。
     */
    private void type3() {
        findViewById(R.id.tv_click_3).setOnClickListener(new ButtonListener());
    }

    /**
     * 4. XML 中 OnClick 方式。
     * 属性名字对应为方法名
     */
    @SensorsDataTrackViewOnClick
    public void type4(View v) {
        Toast.makeText(this, "方式4(OnClick)", Toast.LENGTH_SHORT).show();
    }

    /**
     * 5. lambda 方式。
     */
    private void type5() {
        findViewById(R.id.tv_click_5).setOnClickListener(v -> Toast.makeText(ClickActivity.this, "方式5(Lambda)", Toast.LENGTH_SHORT).show());
    }

    /**
     * 8. dataBinding 方式。
     */
    private void type8() {
        BindingEntity bindingEntity = new BindingEntity("方式8(dataBinding)");
        activityClickBinding.setData(bindingEntity);
        activityClickBinding.setClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ClickActivity.this, "方式8(dataBinding)", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 9. Kotlin 方式。
     */
    private void type9() {
        TextView textView = findViewById(R.id.tv_click_9);
        SensorsDataAPI.sharedInstance().ignoreView(textView);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ClickActivity.this, KotlinActivity.class));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    class ButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Toast.makeText(ClickActivity.this, "方式3", Toast.LENGTH_SHORT).show();
        }
    }
}
