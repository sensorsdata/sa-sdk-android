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

import com.sensorsdata.analytics.android.demo.R;

public class FragmentActivity extends BaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        this.setTitle("Fragment");
        initView();
    }

    private void initView() {
        // androidx
        findViewById(R.id.tv_frg_x_viewpager).setOnClickListener(this);
        findViewById(R.id.tv_frg_x_tab).setOnClickListener(this);
        findViewById(R.id.tv_frg_x_viewpager_viewpager).setOnClickListener(this);
        findViewById(R.id.tv_frg_x_tab_viewpager).setOnClickListener(this);
        // app.Fragment
        findViewById(R.id.tv_frg_app_viewpager).setOnClickListener(this);
        findViewById(R.id.tv_frg_app_tab).setOnClickListener(this);
        findViewById(R.id.tv_viewpager_view).setOnClickListener(this);
        findViewById(R.id.tv_multi_fragments).setOnClickListener(this);
        findViewById(R.id.tv_multi_hori_fragments).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_frg_x_viewpager:
                openVPFragment_x();
                break;
            case R.id.tv_frg_x_tab:
                openTabFrgActivity_x();
                break;
            case R.id.tv_frg_x_viewpager_viewpager:
                openVPVPActivity_x();
                break;
            case R.id.tv_frg_x_tab_viewpager:
                openActivity(TabVPFragActivity.class);
                break;
            case R.id.tv_frg_app_viewpager:
                openVPAppFrgActivity_app();
                break;
            case R.id.tv_frg_app_tab:
                openTabAppFrgActivity_app();
                break;
            case R.id.tv_viewpager_view:
                openVPViewActivity();
                break;
            case R.id.tv_multi_fragments:
                openMultiFragments();
                break;
            case R.id.tv_multi_hori_fragments:
                openMultiHorizFragments();
                break;
            default:
                break;
        }
    }

    /**
     * 打开 VPFrgActivity
     */
    private void openVPFragment_x() {
        startActivity(new Intent(this, VPFrgActivity.class));
    }

    /**
     * 打开 TabFrgActivity
     */
    private void openTabFrgActivity_x() {
        startActivity(new Intent(this, TabFrgActivity.class));
    }

    /**
     * 打开 VPVPFrgActivity
     */
    private void openVPVPActivity_x() {
        startActivity(new Intent(this, VPVPFrgActivity.class));
    }

    /**
     * 打开 TabAppFrgActivity
     */
    private void openTabAppFrgActivity_app() {
        startActivity(new Intent(this, TabAppFrgActivity.class));
    }

    private void openActivity(Class toClass) {
        startActivity(new Intent(this, toClass));
    }

    /**
     * 打开 VPAppFrgActivity
     */
    private void openVPAppFrgActivity_app() {
        startActivity(new Intent(this, VPAppFrgActivity.class));
    }

    /**
     * 打开 VPViewActivity
     */
    private void openVPViewActivity() {
        startActivity(new Intent(this, VPViewActivity.class));
    }

    private void openMultiFragments() {
        startActivity(new Intent(this, VerticalFragmentsActivity.class));
    }

    private void openMultiHorizFragments() {
        startActivity(new Intent(this, HorizonFragmentsActivity.class));
    }
}
