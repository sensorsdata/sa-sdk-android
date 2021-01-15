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

package com.sensorsdata.analytics.android.demo.fragment.androidx;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.activity.TestInnerGridViewAdapter;
import com.sensorsdata.analytics.android.demo.activity.TestRecyclerViewAdapter;
import com.sensorsdata.analytics.android.demo.custom.HorizonRecyclerDivider;
import com.sensorsdata.analytics.android.demo.fragment.BaseAndroidXFragment;

import java.util.ArrayList;
import java.util.List;

public class Frg_4 extends BaseAndroidXFragment {

    public Frg_4() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_frg_4, container, false);
        RecyclerView recyclerView = v.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        recyclerView.addItemDecoration(new HorizonRecyclerDivider(getContext(), HorizonRecyclerDivider.VERTICAL_LIST));
        List list = new ArrayList();
        for (int i = 0; i < 100; i++) {
            list.add(i + "");
        }
        TestRecyclerViewAdapter testListAdapter = new TestRecyclerViewAdapter(getContext(), list);
        recyclerView.setAdapter(testListAdapter);
        return v;
    }
}