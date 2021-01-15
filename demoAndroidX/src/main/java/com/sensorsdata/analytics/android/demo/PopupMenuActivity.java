/*
 * Created by zhangwei on 2019/10/23.
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

package com.sensorsdata.analytics.android.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.sensorsdata.analytics.android.demo.activity.BaseActivity;

import org.jetbrains.annotations.Nullable;

public class PopupMenuActivity extends BaseActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popupmenu);
    }

    public void doOnClick(View view) {
        switch (view.getId()) {
            case R.id.androidWidget: {
                android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, view);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Log.i("11onmenuitemclick", item.getTitle()+"====");
                        return true;
                    }
                });
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
                popupMenu.show();
                break;
            }
            case R.id.androidCompat: {
                androidx.appcompat.widget.PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(this, view);
                popupMenu.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Log.i("22onmenuitemclick", item.getTitle()+"====");
                        return true;
                    }
                });
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
                popupMenu.show();
                break;
            }
            case R.id.lambdaAndroidWidget: {
                android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, view);
                popupMenu.setOnMenuItemClickListener(item -> {
                    Log.i("33onmenuitemclick", item.getTitle()+"====");
                    return true;
                });
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
                popupMenu.show();
                break;
            }
            case R.id.lambdaAndroidCompat: {
                androidx.appcompat.widget.PopupMenu popupMenu = new androidx.appcompat.widget.PopupMenu(this, view);
                popupMenu.setOnMenuItemClickListener(item -> {
                    Log.i("44onmenuitemclick", item.getTitle()+"====");
                    return true;
                });
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
                popupMenu.show();
                break;
            }
        }

    }
}
