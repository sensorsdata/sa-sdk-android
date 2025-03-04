/*
 * Created by dengshiwei on 2022/06/28.
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

package com.sensorsdata.sdk.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sensorsdata.analytics.android.sdk.SensorsDataIgnoreTrackAppClick;
import com.sensorsdata.sdk.demo.adapter.MyAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SensorsDataIgnoreTrackAppClick
public class FuncActivity extends Activity {
    private RecyclerView mRecyclerContainer;
    private Map<String, List<String>> mFuncItems = Map.of(
            "用户关联", List.of("login", "identify", "logout"),
            "用户属性", List.of("profileSet", "profileSetOnce","profileIncrement","profileAppend","profileUnset"),
            "事件埋点", List.of("track代码埋点", "带时长的埋点","物品属性设置","物品属性删除"),
            "事件属性", List.of("获取预置属性", "静态公共属性"),
            "数据存储与发送", List.of("flush", "setMaxCacheSize","setFlushInterval", "deleteAll", "setSSLSocketFactory")
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_func);
        mRecyclerContainer = findViewById(R.id.recyclerView);
        mRecyclerContainer.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerContainer.setAdapter(new MyAdapter(Arrays.asList("用户关联", "用户属性", "事件埋点", "事件属性", "数据存储与发送"), mFuncItems));
    }


}
