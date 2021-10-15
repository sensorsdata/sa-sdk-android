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
import androidx.viewpager.widget.ViewPager;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_1;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_2;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_3;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewPager + android.support.v4.app.Fragment;
 * ViewPager1(展示 Frg_1、Frg_2、Frg_3)
 * 嵌套 ViewPager2(展示 Frg_4、Frg_5、Frg_6)
 */
public class VPVPFrgActivity extends BaseActivity {

    private FragmentPagerAdapter pagerAdapter = null;
    private List<Fragment> listPagerViews = null;

    public FragmentPagerAdapter getFragmentPagerAdapter() {
        return pagerAdapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pger_view_pager);
        this.setTitle("ViewPager + ViewPager + v4 Fragment");
        initViewPager();
    }

    private void initViewPager() {
        listPagerViews = new ArrayList<>();

        listPagerViews.add(new Frg_1());
        listPagerViews.add(new Frg_2());
        listPagerViews.add(new Frg_3());
        ViewPager viewPager1 = (ViewPager) findViewById(R.id.view_pager_activity_vp);
        pagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
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
        viewPager1.setAdapter(pagerAdapter);
    }

}
