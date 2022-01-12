package com.sensorsdata.analytics.android.demo.activity;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;
import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.BaseAndroidXFragment;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_1;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_2;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_3;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_5;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_6;
import com.sensorsdata.analytics.android.demo.fragment.androidx.Frg_7;

public class TabParentFrgActivity1 extends BaseActivity {

    private FragmentManager fragmentManager = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_frg);
        this.setTitle("TabLayout + v4 Fragment");
        initTabLayout();

    }

    public static class TabLayoutHelper {
        private static FragmentManager mFragmentManager;
        private static Frg_6 frg_6 = new Frg_6();
        private static Frg_7 frg_7 = new Frg_7();
        private static Frg_5 frg_5 = new Frg_5();

        public static void init(TabLayout tabLayout, FragmentManager fragmentManager) {
            mFragmentManager = fragmentManager;
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

                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });

            showFrg(frg_7);
        }

        private static void showFrg(BaseAndroidXFragment fragment) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fl_tab_frg, fragment);
            fragmentTransaction.commit();
        }
    }

    /**
     * TabLayout
     */
    private void initTabLayout() {
        TabLayout tabLayout = findViewById(R.id.tablayout_tab_frg);
        fragmentManager = getSupportFragmentManager();
        TabLayoutHelper.init(tabLayout, fragmentManager);
    }
}
