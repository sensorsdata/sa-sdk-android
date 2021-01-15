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

package com.sensorsdata.analytics.android.demo.fragment.app;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.custom.HorizonRecyclerDivider;
import com.sensorsdata.analytics.android.demo.fragment.BaseAppFragment;
import com.sensorsdata.analytics.android.demo.fragment.view.NestRecyclerViewAdapter;

public class Frg_app_3 extends BaseAppFragment {
    private RecyclerView recyclerView;
    public Frg_app_3() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_frg_app_3, container, false);
        recyclerView = v.findViewById(R.id.recyclerView_app3);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false));
        recyclerView.addItemDecoration(new HorizonRecyclerDivider(getActivity(), HorizonRecyclerDivider.VERTICAL_LIST));
        NestRecyclerViewAdapter nestRecyclerViewAdapter = new NestRecyclerViewAdapter(getActivity());
        recyclerView.setAdapter(nestRecyclerViewAdapter);
        return v;
    }
}