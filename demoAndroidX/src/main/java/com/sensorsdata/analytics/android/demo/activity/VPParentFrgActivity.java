package com.sensorsdata.analytics.android.demo.activity;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_5;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_6;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_7;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VPParentFrgActivity extends BaseActivity {
    private List<Fragment> listPagerViews = null;
    private PagerAdapter pagerAdapter = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_viewpager_fragment);
        initViewPager();

    }

    private void initViewPager() {
        listPagerViews = new ArrayList<>();

        listPagerViews.add(new Frg_7());
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
