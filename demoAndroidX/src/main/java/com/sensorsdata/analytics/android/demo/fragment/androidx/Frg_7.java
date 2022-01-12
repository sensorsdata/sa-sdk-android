package com.sensorsdata.analytics.android.demo.fragment.androidx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;
import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.activity.TabParentFrgActivity1;
import com.sensorsdata.analytics.android.demo.fragment.BaseAndroidXFragment;

public class Frg_7 extends BaseAndroidXFragment {

    private FragmentManager fragmentManager;

    public Frg_7() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_frg_7, container, false);
        fragmentManager = getChildFragmentManager();
        initTabLayout(v);
        return v;
    }


    public static class TabLayoutHelper {
        private static FragmentManager mFragmentManager;
        private static Frg_1 frg_1 = new Frg_1();
        private static Frg_2 frg_2 = new Frg_2();
        private static Frg_3 frg_3 = new Frg_3();

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
                            showFrg(frg_1);
                            break;
                        case 1:
                            showFrg(frg_2);
                            break;
                        case 2:
                            showFrg(frg_3);
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
            showFrg(frg_1);
        }

        private static void showFrg(BaseAndroidXFragment fragment) {
            FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_fl_tab_frg, fragment);
            fragmentTransaction.commit();
        }
    }

    /**
     * TabLayout
     *
     * @param v
     */
    private void initTabLayout(View v) {
        TabLayout tabLayout = v.findViewById(R.id.fragment_tablayout_tab_frg);
        TabLayoutHelper.init(tabLayout, fragmentManager);
    }


}
