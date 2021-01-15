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


import android.os.Bundle;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_4;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_5;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_6;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager + androidx.fragment.app.Fragment;
 * 展示 Frg_4、 Frg_5、 Frg_6
 */
public class VPFrgActivity extends BaseActivity {

    private List<Fragment> listPagerViews = null;
    private PagerAdapter pagerAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpfrg);
        this.setTitle("ViewPager + v4 Fragment");
        initViewPager();
    }

    private void initViewPager() {
        listPagerViews = new ArrayList<>();

        listPagerViews.add(new Frg_4());
        listPagerViews.add(new Frg_5());
        listPagerViews.add(new Frg_6());
        ViewPager viewPager = findViewById(R.id.vp_frg);
        pagerAdapter = new FragmentPagerAdapter(this.getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return listPagerViews.get(position);
            }

            @Override
            public int getCount() {
                return listPagerViews.size();
            }

            @Override
            public void setPrimaryItem(ViewGroup container, int position, Object object) {
                super.setPrimaryItem(container, position, object);
            }
        };
        viewPager.setOffscreenPageLimit(1);//不设置缓存页面，获取 ViewPath 会变。ViewPgaer 会 destroyItem
        viewPager.setAdapter(pagerAdapter);
    }
}
