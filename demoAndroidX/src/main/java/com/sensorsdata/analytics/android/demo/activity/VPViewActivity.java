/*
 * Created by zhangxiangwei on 2019/12/12.
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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
 * ViewPager + View;
 */
public class VPViewActivity extends BaseActivity implements View.OnClickListener{

    private List<View> listPagerViews = null;
    private static final String TAG = "VPViewActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vpfrg);
        this.setTitle("ViewPager + view");
        initViewPager();
    }

    private void initViewPager() {
        listPagerViews = new ArrayList<>();

        LayoutInflater lf = getLayoutInflater().from(this);
        View view1 = lf.inflate(R.layout.fragment_frg_1, null);
        View view2 = lf.inflate(R.layout.fragment_frg_2, null);
        View view3 = lf.inflate(R.layout.fragment_frg_3, null);
        View view4 = lf.inflate(R.layout.fragment_frg_4, null);
        View view5 = lf.inflate(R.layout.fragment_frg_5, null);
        View view6 = lf.inflate(R.layout.fragment_frg_6, null);
        view1.setOnClickListener(this);
        view2.setOnClickListener(this);
        view3.setOnClickListener(this);
        view4.setOnClickListener(this);
        view5.setOnClickListener(this);
        view6.setOnClickListener(this);
        listPagerViews.add(view1);
        listPagerViews.add(view2);
        listPagerViews.add(view3);
        listPagerViews.add(view4);
        listPagerViews.add(view5);
        listPagerViews.add(view6);
        ViewPager viewPager = findViewById(R.id.vp_frg);
        viewPager.setAdapter(new MyPagerAdapter(listPagerViews));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void onClick(View v) {

    }


    public class MyPagerAdapter extends PagerAdapter {

        private List<View> mListViews;

        public MyPagerAdapter(List<View> mListViews) {
            this.mListViews = mListViews;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mListViews.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mListViews.get(position), 0);
            return mListViews.get(position);
        }

        @Override
        public int getCount() {
            return mListViews.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }


    }

}
