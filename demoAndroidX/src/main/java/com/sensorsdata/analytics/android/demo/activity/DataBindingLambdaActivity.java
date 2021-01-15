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

package com.sensorsdata.analytics.android.demo.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.databinding.DataBindingUtil;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.databinding.ActivityDbLBinding;
import com.sensorsdata.analytics.android.sdk.PropertyBuilder;

public class DataBindingLambdaActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityDbLBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_db_l);
        binding.setHandlers(this);
        testLambdaButton();
        testCompoundButton();
        testDialog();
        testListViewItemClick();
        testTabHost();
        testNavigationViewItemClick();
        testExpandListView();
        testRatingBar();
    }

    private void testExpandListView() {
        //可以参考 08 ListView & ExpaadableListView
        ExpandableListView expandableListView = new ExpandableListView(this);
        expandableListView.setOnGroupClickListener((expandableListView1, view, i, l) -> false);
        expandableListView.setOnChildClickListener((expandableListView12, view, i, i1, l) -> false);
    }

    public void onViewClick(View view) {
    }

    private void testLambdaButton() {
        Button button = findViewById(R.id.lambdaButton);
        button.setOnClickListener(v -> {
            PropertyBuilder.newInstance().append("ss", "sdf").append("sds", "sdf").toJSONObject();
        });
    }

    private void testCompoundButton() {
        SwitchCompat switchCompat = findViewById(R.id.switch_compat);
        switchCompat.setOnCheckedChangeListener((compoundButton, b) -> {

        });
    }

    private void testDialog() {
        findViewById(R.id.dialog).setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("问题：");
            builder.setMessage("请问你满十八岁了吗?");
            builder.setIcon(R.mipmap.ic_launcher);
            //点击对话框以外的区域是否让对话框消失
            builder.setCancelable(true);
            //设置正面按钮
            builder.setPositiveButton("是的", (dialog, which) -> {
                Toast.makeText(this, "你点击了是的", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            //设置反面按钮
            builder.setNegativeButton("不是", (dialog, which) -> {
                Toast.makeText(this, "你点击了不是", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            //设置中立按钮
            builder.setNeutralButton("保密", (dialog, which) -> {
                Toast.makeText(this, "你选择了保密", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            builder.show();
        });
    }

    private void testRatingBar() {
        AppCompatRatingBar ratingBar = findViewById(R.id.ratingbar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {

            }
        });
    }

    private void testListViewItemClick() {
        String[] strArr = new String[]{"yuhongxing", "sunshengling",
                "chenyanzhang", "huangchao", "liupengfei"};
        ListView listView = (ListView) findViewById(R.id.listview);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, strArr);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((adapterView, view, i, l) -> {

        });
    }

    private void testTabHost() {
        findViewById(R.id.tabHost).setOnClickListener(view -> {
            Intent intent = new Intent(this, MyTabHostActivity.class);
            startActivity(intent);
        });
    }

    private void testNavigationViewItemClick() {
        findViewById(R.id.navigation).setOnClickListener(view -> {
            Intent intent = new Intent(this, NavigationViewActivity.class);
            startActivity(intent);
        });
    }


}
