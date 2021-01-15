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

package com.sensorsdata.analytics.android.demo.fragment.androidx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.BaseAndroidXFragment;

import java.util.ArrayList;
import java.util.List;

public class Frg_2 extends BaseAndroidXFragment {

    private static final String TAG = "SA.Sensors.ViewPger222";
    private static FragmentPagerAdapter pagerAdapter = null;
    private View view = null;
    private List<Fragment> listPagerViews = null;

    public Frg_2() {
    }

    public static FragmentPagerAdapter getFragmentPagerAdapter() {
        return pagerAdapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_frg_2, container, false);
        view.findViewById(R.id.tv_frg_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Frg_2 点击", Toast.LENGTH_SHORT).show();
            }
        });
        initViewPager();
        return view;
    }

    private void initViewPager() {
        listPagerViews = new ArrayList<>();
        listPagerViews.add(new Frg_4());
        listPagerViews.add(new Frg_5());
        listPagerViews.add(new Frg_6());
        ViewPager viewPager2 = (ViewPager) view.findViewById(R.id.frg_2_vp);
        pagerAdapter = new FragmentPagerAdapter(this.getChildFragmentManager()) {
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
        viewPager2.setAdapter(pagerAdapter);
    }
}