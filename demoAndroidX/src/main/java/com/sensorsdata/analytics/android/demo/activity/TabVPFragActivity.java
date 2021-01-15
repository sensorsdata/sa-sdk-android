/*
 * Created by chenru on 2019/07/24.
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
import android.view.ViewParent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_1;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_2;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_3;

import java.util.ArrayList;
import java.util.List;

public class TabVPFragActivity extends AppCompatActivity {
    TabLayout tabLayout;
    ViewPager viewPager;

    List<Fragment> fragments = new ArrayList<>();
    List<String> titles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_viewpager);

        tabLayout = findViewById(R.id.tl_tabs);
        viewPager = findViewById(R.id.vp_content);

        fragments.add(new Frg_1());
        fragments.add(new Frg_2());
        fragments.add(new Frg_3());
        titles.add("fragment1");
        titles.add("fragment2");
        titles.add("fragment3");
        viewPager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragments.get(position);
            }

            @Override
            public int getCount() {
                return fragments.size();
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                super.destroyItem(container, position, object);
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return titles.get(position);
            }
        });

        tabLayout.setupWithViewPager(viewPager);
    }

    private ViewPager findViewGroup(ViewParent parent) {
        if (parent == null) {
            return null;
        }
        if (parent instanceof ViewPager) {
            return (ViewPager) parent;
        }
        return findViewGroup(parent.getParent());
    }
}