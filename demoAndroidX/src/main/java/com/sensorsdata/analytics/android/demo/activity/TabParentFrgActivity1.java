package com.sensorsdata.analytics.android.demo.activity;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;
import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.BaseAndroidXFragment;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_5;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_6;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_7;

public class TabParentFrgActivity1 extends BaseActivity {

    private FragmentManager fragmentManager = null;
    private FragmentTransaction fragmentTransaction = null;
    private Frg_7 frg_7 = null;
    private Frg_5 frg_5 = null;
    private Frg_6 frg_6 = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_frg);
        this.setTitle("TabLayout + v4 Fragment");
        initFragment(savedInstanceState);
        initTabLayout();
    }

    private void initFragment(Bundle savedInstanceState) {
        fragmentManager = this.getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        if (savedInstanceState != null) {
            frg_7 = (Frg_7) fragmentManager.findFragmentByTag(Frg_7.class.getSimpleName());
            frg_5 = (Frg_5) fragmentManager.findFragmentByTag(Frg_5.class.getSimpleName());
            frg_6 = (Frg_6) fragmentManager.findFragmentByTag(Frg_6.class.getSimpleName());
        } else {
            frg_7 = new Frg_7();
            frg_5 = new Frg_5();
            frg_6 = new Frg_6();
//            fragmentTransaction.add(R.id.fl_tab_frg, frg_4, Frg_4.class.getSimpleName()).hide(frg_4);
//            fragmentTransaction.add(R.id.fl_tab_frg, frg_5, Frg_5.class.getSimpleName()).hide(frg_5);
//            fragmentTransaction.add(R.id.fl_tab_frg, frg_6, Frg_6.class.getSimpleName()).hide(frg_6);
//            fragmentTransaction.commit();
        }
        showFrg(frg_7);
    }

    /**
     * TabLayout
     */
    private void initTabLayout() {
        TabLayout tabLayout = findViewById(R.id.tablayout_tab_frg);
        String[] arr = {"首页", "分类", "朋友"};
        for (String title : arr) {
            TabLayout.Tab tab = tabLayout.newTab();
            tab.setText(title);
            tabLayout.addTab(tab);
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        showFrg(frg_7);
                        break;
                    case 1:
                        showFrg(frg_5);
                        break;
                    case 2:
                        showFrg(frg_6);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void showFrg(BaseAndroidXFragment fragment) {
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fl_tab_frg,fragment);
        fragmentTransaction.commit();
    }

}
