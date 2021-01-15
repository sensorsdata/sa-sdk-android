/*
 * Created by dengshiwei on 2019/12/25.
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

import android.app.Fragment;
import android.os.Bundle;

import androidx.legacy.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.BaseAppFragment;
import com.sensorsdata.analytics.android.demo.fragment.app.Frg_app_1;
import com.sensorsdata.analytics.android.demo.fragment.app.Frg_app_2;
import com.sensorsdata.analytics.android.demo.fragment.app.Frg_app_3;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager + android.app.Fragment
 * 展示 Frg_app_1、Frg_app_2、Frg_app_3
 */
public class VPAppFrgActivity extends BaseActivity {

    private List<BaseAppFragment> listPagerViews = null;
    private FragmentPagerAdapter pagerAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpapp_frg);
        this.setTitle("ViewPager + app.Fragment");
        initViewPager();
    }

    private void initViewPager() {
        listPagerViews = new ArrayList<>();

        listPagerViews.add(new Frg_app_1());
        listPagerViews.add(new Frg_app_2());
        listPagerViews.add(new Frg_app_3());
        ViewPager viewPager = findViewById(R.id.vp_app_frg);
        pagerAdapter = new FragmentPagerAdapter(getFragmentManager()) {
            @Override
            public int getCount() {
                return listPagerViews.size();
            }

            @Override
            public Fragment getItem(int i) {
                return listPagerViews.get(i);
            }
        };
        viewPager.setAdapter(pagerAdapter);
    }
}
