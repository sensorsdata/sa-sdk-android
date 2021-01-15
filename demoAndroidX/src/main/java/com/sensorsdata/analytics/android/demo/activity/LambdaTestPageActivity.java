/*
 * Created by zhangwei on 2020/08/17.
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

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.sensorsdata.analytics.android.demo.R;

public class LambdaTestPageActivity extends AppCompatActivity {
    private static final String TAG = "LambdaTestPageActivity";
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lambda);
        init();
    }

    private void init() {
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View view, float v) {

            }

            @Override
            public void onDrawerOpened(@NonNull View view) {
                toast("DrawerOpened");
            }

            @Override
            public void onDrawerClosed(@NonNull View view) {
                toast("DrawerClosed");
            }

            @Override
            public void onDrawerStateChanged(int i) {

            }
        });

        mNavigationView = findViewById(R.id.nav_view);
        handleToolbar();
        handleNavigation();
        handleButton();
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.navi, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }

    private void handleButton() {
        findViewById(R.id.button).setOnClickListener(v -> {
            toast("Button 点击");
        });
        findViewById(R.id.appcompatButton).setOnClickListener(v -> {
            toast("AppCompatButton 点击");
        });
        CheckBox checkBox =  findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> toast("CheckBox onCheckedChanged"));
        RadioButton radioButton = findViewById(R.id.radio);
        radioButton.setOnClickListener(v -> {
            toast("RadioButton 点击");
        });
        AppCompatRatingBar appCompatRatingBar = findViewById(R.id.compatRatingBar);
        appCompatRatingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> toast("AppCompatRatingBar onRatingChanged"));

        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                toast("SeekBar onProgressChanged");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                toast("SeekBar onStartTrackingTouch");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                toast("SeekBar onStopTrackingTouch");
            }
        });

        Switch switchButton = findViewById(R.id.switchButton);
        switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> toast("Switch onCheckedChanged"));

        findViewById(R.id.buttonAlertDialog).setOnClickListener(v->{handleDialog();});

        findViewById(R.id.buttonPopupMenu).setOnClickListener( v->{handlePopupMenu(findViewById(R.id.buttonPopupMenu));});
        handleExpandableListView();
        handleBottomNavigation();
    }

    private void handleToolbar() {
        mToolbar.setTitle("测试 Lambda");
        mToolbar.setNavigationIcon(R.drawable.ic_menu_slideshow);
        mToolbar.setSubtitle("侧滑");
        mToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_click) {
                toast("Toolbar 点击");
            }
            return false;
        });
        mToolbar.setNavigationOnClickListener(v -> {
            toast("Toolbar Navigation 点击");
            mDrawerLayout.openDrawer(Gravity.RIGHT);
        });
    }

    private void handleNavigation() {
        mNavigationView.setNavigationItemSelectedListener(menuItem -> {
            toast("Navigation 点击");
            mDrawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void handleDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("AlertDialog");
        alertDialogBuilder.setMessage("AlertDialog Message");
        alertDialogBuilder.setPositiveButton("ok", (dialog, which) -> toast("AlertDialog ok"));
        alertDialogBuilder.setOnCancelListener(dialog -> {
            toast("AlertDialog Cancel");
        });
        alertDialogBuilder.create().show();
    }

    private void handlePopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(item -> {
            toast("PopupMenu onMenuItemClick");
            return false;
        });
    }

    private void handleExpandableListView() {
        final String[] groups = new String[]{"神策数据", "神策数据", "神策数据", "神策数据"};
        final String[][] childs = new String[][]{{"Sensors Analytics", "Sensors Focus", "Sensors Personas", "Sensors Recommender", "Sensors Journey"}, {"Sensors Data Governor", "Sensors Point", "Sensors Personalization Engine",
                "Sensors Platform"}, {"Sensors School", "Sensors Data Library", "SA 分析师认证", "SA 分析专家认证"}, {"Sensors Data Open Source", "Sensors Data"}};
        ExpandableListView expandableListView = findViewById(R.id.expandableListView);
        expandableListView.setAdapter(new BaseExpandableListAdapter() {
            @Override
            public int getGroupCount() {
                return groups.length;
            }

            @Override
            public int getChildrenCount(int groupPosition) {
                return childs[groupPosition].length;
            }

            @Override
            public Object getGroup(int groupPosition) {
                return groups[groupPosition];
            }

            @Override
            public Object getChild(int groupPosition, int childPosition) {
                return childs[groupPosition][childPosition];
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
                return false;
            }

            @Override
            public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
                TextView textView = new TextView(LambdaTestPageActivity.this);
                textView.setText(groups[groupPosition]);
                textView.setPadding(20,5,5,5);
                textView.setTextColor(Color.BLACK);
                textView.setTextSize(24);
                return textView;
            }

            @Override
            public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
                TextView textView = new TextView(LambdaTestPageActivity.this);
                textView.setText(childs[groupPosition][childPosition]);
                textView.setTextColor(Color.BLUE);
                textView.setPadding(40,5,5,5);
                textView.setTextSize(18);
                return textView;
            }

            @Override
            public boolean isChildSelectable(int groupPosition, int childPosition) {
                return true;
            }
        });
        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            toast("ExpandableListView onGroupClick");
            return false;
        });
        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            toast("ExpandableListView onChildClick");
            return false;
        });
    }

    private void handleBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            toast("BottomNavigationView ItemSelected");
            return true;
        });
    }

    private void toast(String tip) {
        Toast.makeText(LambdaTestPageActivity.this, tip, Toast.LENGTH_SHORT).show();
    }

    private long age = 99;
    private String name = "xiaoming";
    private int yui = 88;

    private void lambdaTest() {
        //1
        View view1 = new View(this);
        int a = 10;
        long b = 11;
        view1.setOnClickListener(view -> {
            System.out.println("hello world===" + b + (a + age));
        });

        //2
        CheckBox checkBox = new CheckBox(this);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            System.out.println("hello world===" + name + "==" + age + "==" + 88 + "===" + a + b);
        });

        //3
        RatingBar ratingBar = new RatingBar(this);
        ratingBar.setOnRatingBarChangeListener((ratingBar1, rating, fromUser) -> {
            String tmp = this.name;
        });

        //4 不属于 lambda 范畴
        SeekBar seekBar = new SeekBar(this);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //5
        RadioGroup radioGroup = new RadioGroup(this);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {

        });

        //6,16
        AlertDialog dialog = new AlertDialog.Builder(this).setPositiveButton("", (dialog1, which) -> {

        }).setMultiChoiceItems(1, null, (dialog12, which, isChecked) -> {

        }).show();


        //7
        ListView listView = new ListView(this);
        listView.setOnItemClickListener((parent, view, position, id) -> {

        });

        //8
        ExpandableListView expandableListView = new ExpandableListView(this);
        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> false);
        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> false);

        //9
        TabHost tabHost = new TabHost(this);
        tabHost.setOnTabChangedListener(tabId -> {

        });


        //10
        NavigationView navigationView = new NavigationView(this);
        navigationView.setNavigationItemSelectedListener(item -> false);

        //11
        //todo  support NavigationView


        //12
        BottomNavigationView bottomNavigationView = new BottomNavigationView(this);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> false);

        //13
        //todo support BottomNavigationView

        //14
        Toolbar toolbar = new Toolbar(this);
        toolbar.setOnMenuItemClickListener(item -> false);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return false;
            }
        });

        //15
        //todo appcompat ToolBar

        //17
        PopupMenu popupMenu = new PopupMenu(this, toolbar);
        popupMenu.setOnMenuItemClickListener(item -> ttt(item));
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return false;
            }
        });

        //18
        androidx.appcompat.widget.PopupMenu popupMenu1 = new androidx.appcompat.widget.PopupMenu(this, toolbar);
        popupMenu1.setOnMenuItemClickListener(item -> false);
        popupMenu1.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return false;
            }
        });

        //19 todo support PopupMenu

    }

    private boolean ttt(MenuItem item){
        return true;
    }

}
