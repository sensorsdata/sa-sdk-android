/*
 * Created by dengshiwei on 2022/07/11.
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

package com.sensorsdata.analytics.android.autotrack.aop;

import static com.sensorsdata.analytics.android.autotrack.core.business.SAPageTools.getActivityTitle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.material.tabs.TabLayout;
import com.sensorsdata.analytics.android.autotrack.SAHelper;
import com.sensorsdata.analytics.android.autotrack.core.impl.AutoTrackProtocolIml;
import com.sensorsdata.analytics.android.sdk.AopConstants;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackEventCallBack;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboMenuItem;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class SensorsDataAutoTrackHelperTest {
    public String[] groupStrings = {"西游记", "水浒传", "三国演义"};
    public String[][] childStrings = {
            {"唐三藏", "孙悟空", "猪八戒", "沙和尚"},
            {"宋江", "林冲", "李逵", "鲁智深"},
            {"贾宝玉", "林黛玉", "薛宝钗", "王熙凤"}
    };
    Application mApplication = ApplicationProvider.getApplicationContext();
    SensorsDataAPI mSensorsDataAPI = null;

    @Test
    public void testAutoTrackHelper() {
        trackRN();
        trackExpandableListViewOnGroupClick();
        trackExpandableListViewOnChildClick();
        trackTabHost();
        trackTabLayoutSelected();
        trackMenuItem();
        trackRadioGroup();
        trackDialog();
        trackListView();
        trackDrawerOpened();
        trackDrawerClosed();
        trackViewOnClick();
        track();
    }

    public SensorsDataAPI setUp() {
        mSensorsDataAPI = null;
        mSensorsDataAPI = SAHelper.initWithAppClick(mApplication);
        List<SensorsDataAPI.AutoTrackEventType> list = new ArrayList<>();
        list.add(SensorsDataAPI.AutoTrackEventType.APP_START);
        list.add(SensorsDataAPI.AutoTrackEventType.APP_VIEW_SCREEN);
        list.add(SensorsDataAPI.AutoTrackEventType.APP_END);
        mSensorsDataAPI.disableAutoTrack(list);
        return mSensorsDataAPI;
    }

    public void trackRN() {
        assertTrue(true);
    }

    public void trackExpandableListViewOnGroupClick() {
        SensorsDataAPI sensorsDataAPI = setUp();
        final AutoTrackActivity activity = Robolectric.setupActivity(AutoTrackActivity.class);
        ExpandableListView expandableListView = new ExpandableListView(activity);
        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                SensorsDataAutoTrackHelper.trackExpandableListViewOnGroupClick(parent, v, groupPosition);
                return true;
            }
        });
        sensorsDataAPI.setTrackEventCallBack(null);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                System.out.println("trackExpandableListViewOnGroupClick");
                System.out.println(eventName);
                System.out.println(eventProperties);
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppClick", eventName);
                assertEquals("ExpandableListView", eventProperties.optString(AopConstants.ELEMENT_TYPE));
                assertEquals(activity.getClass().getCanonicalName(), eventProperties.optString(AopConstants.SCREEN_NAME));
                assertEquals(getActivityTitle(activity), eventProperties.optString(AopConstants.TITLE));
                return true;
            }
        });
        expandableListView.setAdapter(new MyAdapter());
        expandableListView.performItemClick(expandableListView, 1, -1);
    }

    public void trackExpandableListViewOnChildClick() {
        SensorsDataAPI sensorsDataAPI = setUp();
        final AutoTrackActivity activity = Robolectric.setupActivity(AutoTrackActivity.class);
        ExpandableListView expandableListView = new ExpandableListView(activity);
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                SensorsDataAutoTrackHelper.trackExpandableListViewOnChildClick(parent, v, groupPosition, childPosition);
                return true;
            }
        });
        sensorsDataAPI.setTrackEventCallBack(null);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                System.out.println(eventName);
                System.out.println(eventProperties);
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppClick", eventName);
                assertEquals("ExpandableListView", eventProperties.optString(AopConstants.ELEMENT_TYPE));
                assertEquals(activity.getClass().getCanonicalName(), eventProperties.optString(AopConstants.SCREEN_NAME));
                assertEquals(getActivityTitle(activity), eventProperties.optString(AopConstants.TITLE));
                return true;
            }
        });
        expandableListView.setAdapter(new MyAdapter());
        expandableListView.performItemClick(expandableListView, 4, -1);
    }

    public void trackTabHost() {
        SensorsDataAPI sensorsDataAPI = setUp();
        final AutoTrackActivity activity = Robolectric.setupActivity(AutoTrackActivity.class);
        TabHost tabHost = new TabHost(activity);
        tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("未接来电", null).setContent(100));
        tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("已接来电", null).setContent(200));
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                SensorsDataAutoTrackHelper.trackTabHost(tabId);
            }
        });
        sensorsDataAPI.setTrackEventCallBack(null);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                System.out.println("trackTabHost");
                System.out.println(eventName);
                System.out.println(eventProperties);
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppClick", eventName);
                assertEquals("TabHost", eventProperties.optString(AopConstants.ELEMENT_TYPE));
                assertEquals(activity.getClass().getCanonicalName(), eventProperties.optString(AopConstants.SCREEN_NAME));
                assertEquals(getActivityTitle(activity), eventProperties.optString(AopConstants.TITLE));
                assertEquals("未接来电", eventProperties.optString(AopConstants.ELEMENT_CONTENT));
                return true;
            }
        });
        tabHost.setCurrentTab(0);
    }

    public void trackTabLayoutSelected() {
//        final SensorsDataAPI sensorsDataAPI = SAHelper.initSensors(mApplication);
//        final AppCompatActivity activity = Robolectric.setupActivity(AppCompatActivity.class);
//        final TabLayout tabLayout = new TabLayout(activity);
//        tabLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 60));
//        //tab可滚动
//        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
//        //tab居中显示
//        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);
//        //tab的字体选择器,默认黑色,选择时红色
//        tabLayout.setSelectedTabIndicatorColor(Color.BLUE);
//        TabLayout.Tab tab1 = tabLayout.newTab().setText("Tab 1");
//        tabLayout.addTab(tab1);
//        tabLayout.addTab(tabLayout.newTab().setText("Tab 2"));
//        tabLayout.addTab(tabLayout.newTab().setText("Tab 3"));
//        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
//            @Override
//            public void onTabSelected(TabLayout.Tab tab) {
//                SensorsDataAutoTrackHelper.trackTabLayoutSelected(this, tab);
//            }
//
//            @Override
//            public void onTabUnselected(TabLayout.Tab tab) {
//
//            }
//
//            @Override
//            public void onTabReselected(TabLayout.Tab tab) {
//
//            }
//        });
//        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
//            @Override
//            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
//                assertEquals("$AppClick", eventName);
//                assertEquals("TabLayout", eventProperties.optString(AopConstants.ELEMENT_TYPE));
//                assertEquals(activity.getClass().getCanonicalName(), eventProperties.optString(AopConstants.SCREEN_NAME));
//                assertEquals(getActivityTitle(activity), eventProperties.optString(AopConstants.TITLE));
//                assertEquals("Tab 1", eventProperties.optString(AopConstants.ELEMENT_CONTENT));
//                return true;
//            }
//        });
//        tabLayout.setSelected(true);
//        tab1.select();
    }

    public void trackMenuItem() {
        testTrackMenuItem();
    }

    public void testTrackMenuItem() {
        final SensorsDataAPI sensorsDataAPI = setUp();
        final AutoTrackActivity activity = Robolectric.setupActivity(AutoTrackActivity.class);
        MenuItem menuItem = new RoboMenuItem(activity);
        activity.onOptionsItemSelected(menuItem);
        SensorsDataAutoTrackHelper.trackMenuItem(menuItem);
    }

    public void trackRadioGroup() {
        final SensorsDataAPI sensorsDataAPI = setUp();
        final AutoTrackActivity activity = Robolectric.setupActivity(AutoTrackActivity.class);
        RadioGroup radioGroup = new RadioGroup(activity);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SensorsDataAutoTrackHelper.trackRadioGroup(group, checkedId);
            }
        });
        for (int i = 0; i < 3; i++) {
            RadioButton radioButton = new RadioButton(activity);
            RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(15, 0, 0, 0);
            radioButton.setButtonDrawable(android.R.drawable.btn_radio);
            radioButton.setPadding(40, 0, 0, 0);
            radioButton.setText("radioButton" + i);
            radioButton.setId(i);
            radioGroup.addView(radioButton);
        }
        sensorsDataAPI.setTrackEventCallBack(null);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                System.out.println("trackRadioGroup");
                System.out.println(eventName);
                System.out.println(eventProperties);
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppClick", eventName);
                assertEquals("RadioButton", eventProperties.optString(AopConstants.ELEMENT_TYPE));
                assertEquals(activity.getClass().getCanonicalName(), eventProperties.optString(AopConstants.SCREEN_NAME));
                assertEquals(getActivityTitle(activity), eventProperties.optString(AopConstants.TITLE));
                assertEquals("radioButton0", eventProperties.optString(AopConstants.ELEMENT_CONTENT));
                return true;
            }
        });
        radioGroup.check(0);
    }

    public void trackDialog() {
        final SensorsDataAPI sensorsDataAPI = setUp();
        final AutoTrackActivity activity = Robolectric.setupActivity(AutoTrackActivity.class);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        AlertDialog alert = builder.setIcon(android.R.drawable.btn_radio)
                .setTitle("系统提示：")
                .setMessage("这是一个 AlertDialog,\n带有 2 个按钮，分别是取消和确定")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();
        //alert.show();
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                System.out.println("trackDialog");
                System.out.println(eventName);
                System.out.println(eventProperties);
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppClick", eventName);
                assertEquals("autoTrack", eventProperties.optString("$lib_method"));
                assertEquals("Dialog", eventProperties.optString(AopConstants.ELEMENT_TYPE));
                return true;
            }
        });
        SensorsDataAutoTrackHelper.trackDialog(alert, DialogInterface.BUTTON_NEGATIVE);
    }

    public void trackListView() {
        List<String> list = new ArrayList<>();
        list.add("ArrayItem1");
        list.add("ArrayItem2");
        list.add("ArrayItem3");
        list.add("ArrayItem4");
        final SensorsDataAPI sensorsDataAPI = setUp();
        final AutoTrackActivity activity = Robolectric.setupActivity(AutoTrackActivity.class);
        ListView listView = new ListView(activity);
        ArrayAdapter mArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.activity_list_item, list);
        listView.setAdapter(mArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SensorsDataAutoTrackHelper.trackListView(parent, view, position);
            }
        });
        TextView tv = new TextView(activity);
        tv.setText("Item1");
        listView.performItemClick(tv, 0, android.R.id.text1);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppClick", eventName);
                assertEquals("autoTrack", eventProperties.optString("$lib_method"));
                assertEquals("ListView", eventProperties.optString(AopConstants.ELEMENT_TYPE));
                assertEquals("Item1", eventProperties.optString(AopConstants.ELEMENT_CONTENT));
                return true;
            }
        });
    }

    public void trackDrawerOpened() {
        final SensorsDataAPI sensorsDataAPI = setUp();
        final AutoTrackActivity activity = Robolectric.setupActivity(AutoTrackActivity.class);
        DrawerLayout drawerLayout = new DrawerLayout(activity);
        drawerLayout.setPressed(true);
        SensorsDataAutoTrackHelper.trackDrawerOpened(drawerLayout);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppClick", eventName);
                assertEquals("autoTrack", eventProperties.optString("$lib_method"));
                assertEquals("Open", eventProperties.optString(AopConstants.ELEMENT_CONTENT));
                return true;
            }
        });
    }

    public void trackDrawerClosed() {
        final SensorsDataAPI sensorsDataAPI = setUp();
        final AutoTrackActivity activity = Robolectric.setupActivity(AutoTrackActivity.class);
        DrawerLayout drawerLayout = new DrawerLayout(activity);
        drawerLayout.setPressed(true);
        SensorsDataAutoTrackHelper.trackDrawerClosed(drawerLayout);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                System.out.println(eventName);
                System.out.println(eventProperties);
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppClick", eventName);
                assertEquals("autoTrack", eventProperties.optString("$lib_method"));
                assertEquals("Close", eventProperties.optString(AopConstants.ELEMENT_CONTENT));
                return true;
            }
        });
    }

    public void trackViewOnClick() {
        final SensorsDataAPI sensorsDataAPI = setUp();
        final AutoTrackActivity activity = Robolectric.setupActivity(AutoTrackActivity.class);
        Button button = new Button(activity);
        button.setText("trackViewOnClick");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SensorsDataAutoTrackHelper.trackViewOnClick(v);
            }
        });
        sensorsDataAPI.setTrackEventCallBack(null);
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("$AppClick", eventName);
                assertEquals("autoTrack", eventProperties.optString("$lib_method"));
                assertEquals("Button", eventProperties.optString(AopConstants.ELEMENT_TYPE));
                assertEquals("trackViewOnClick", eventProperties.optString(AopConstants.ELEMENT_CONTENT));
                return true;
            }
        });
        button.performClick();
    }

    public void testTrackViewOnClick() {
        trackViewOnClick();
    }

    public void track() {
        final SensorsDataAPI sensorsDataAPI = setUp();
        sensorsDataAPI.setTrackEventCallBack(new SensorsDataTrackEventCallBack() {
            @Override
            public boolean onTrackEvent(String eventName, JSONObject eventProperties) {
                assertTrue(SAHelper.checkPresetProperty(eventProperties));
                assertEquals("AnnoTestTest", eventName);
                assertEquals("code", eventProperties.optString("$lib_method"));
                return true;
            }
        });
        SensorsDataAutoTrackHelper.track("AnnoTestTest", "");
    }

    class MyAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return groupStrings.length;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return childStrings.length;
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groupStrings[groupPosition];
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return childStrings[groupPosition][childPosition];
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            TextView tv = new TextView(parent.getContext());
            tv.setText(groupStrings[groupPosition]);
            return tv;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            TextView tv = new TextView(parent.getContext());
            tv.setText(childStrings[childPosition][groupPosition]);
            return tv;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    static class AutoTrackActivity extends Activity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTheme(android.R.style.Theme_Material);
        }
    }

    static class SAAppCompatActivity extends androidx.appcompat.app.AppCompatActivity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
    }
}