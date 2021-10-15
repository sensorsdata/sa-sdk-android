package com.sensorsdata.analytics.android.demo.activity;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.BaseAppFragment;
import com.sensorsdata.analytics.android.demo.fragment.app.Frg_app_1;
import com.sensorsdata.analytics.android.demo.fragment.app.Frg_app_2;
import com.sensorsdata.analytics.android.demo.fragment.app.Frg_app_3;
import com.sensorsdata.analytics.android.demo.fragment.app.Frg_app_4;

import org.jetbrains.annotations.Nullable;

public class TabAppParentFrgActivity extends BaseActivity {


    private FragmentManager fragmentManager = null;
    private FragmentTransaction fragmentTransaction = null;
    private Frg_app_4 frg_app_4 = null;
    private Frg_app_2 frg_app_2 = null;
    private Frg_app_3 frg_app_3 = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_app_frg);
        this.setTitle("TabLayout + app.Fragment");
        initFragment(savedInstanceState);
        initTabLayout();
    }

    private void initFragment(Bundle savedInstanceState) {
        fragmentManager = this.getFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        if (savedInstanceState != null) {
            frg_app_4 = (Frg_app_4) fragmentManager.findFragmentByTag(Frg_app_4.class.getSimpleName());
            frg_app_2 = (Frg_app_2) fragmentManager.findFragmentByTag(Frg_app_2.class.getSimpleName());
            frg_app_3 = (Frg_app_3) fragmentManager.findFragmentByTag(Frg_app_3.class.getSimpleName());
        } else {
            frg_app_4 = new Frg_app_4();
            frg_app_2 = new Frg_app_2();
            frg_app_3 = new Frg_app_3();
            fragmentTransaction.add(R.id.fl_tab_app_frg, frg_app_4, Frg_app_1.class.getSimpleName()).hide(frg_app_4);
            fragmentTransaction.add(R.id.fl_tab_app_frg, frg_app_2, Frg_app_2.class.getSimpleName()).hide(frg_app_2);
            fragmentTransaction.add(R.id.fl_tab_app_frg, frg_app_3, Frg_app_3.class.getSimpleName()).hide(frg_app_3);
            fragmentTransaction.commit();
        }
        showFrg(frg_app_4);
    }

    /**
     * TabLayout
     */
    private void initTabLayout() {
        TabLayout tabLayout = findViewById(R.id.tablayout_tab_app_frg);
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
                        showFrg(frg_app_4);
                        break;
                    case 1:
                        showFrg(frg_app_2);
                        break;
                    case 2:
                        showFrg(frg_app_3);
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

    private void showFrg(BaseAppFragment fragment) {
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.hide(frg_app_4);
        fragmentTransaction.hide(frg_app_2);
        fragmentTransaction.hide(frg_app_3);
        fragmentTransaction.show(fragment);
        fragmentTransaction.commit();
    }
}
