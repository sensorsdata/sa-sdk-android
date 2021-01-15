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

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sensorsdata.analytics.android.demo.R;
import com.sensorsdata.analytics.android.demo.fragment.BaseAppFragment;

import java.util.ArrayList;

public class Frg_app_2 extends BaseAppFragment {
    private LinearLayout group_container;
    public Frg_app_2() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_frg_app_2, container, false);
        v.findViewById(R.id.tv_app_frg_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dynamicAddView();
            }
        });
        group_container = v.findViewById(R.id.group_container);
        return v;
    }

    GridView gridView;
    private void dynamicAddView() {
        // 动态添加 Button
        Context context = getActivity();
        View view = LayoutInflater.from(context).inflate(R.layout.layout_button, null);
        LinearLayout linearGroup = view.findViewById(R.id.liner_group);
        Button button = view.findViewById(R.id.btn_dynamivbutton);//new Button(getActivity());
        button.setText("动态添加的 Button");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "点击已触发", Toast.LENGTH_SHORT).show();
            }
        });

        //动态添加 ListView
        ListView adapterView = view.findViewById(R.id.listView);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_expandable_list_item_1, getData());
        adapterView.setAdapter(adapter);
        adapterView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Toast.makeText(getActivity(), "点击已触发", Toast.LENGTH_SHORT).show();
                printGridView(gridView);
            }
        });

        gridView = view.findViewById(R.id.gridView);
        gridView.setNumColumns(4);
        gridView.setAdapter(new MyListAdapter());
        group_container.addView(view);
        printGridView(gridView);
        Log.d("Andoter", "gridview = " + gridView.getRootView());
    }

    private void printGridView(GridView gridView) {
        int count = gridView.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = gridView.getChildAt(i);
            Log.d("dsw", view.toString());
        }
        Log.d("dsw", "end");
    }

    private ArrayList<String> list = new ArrayList<String>();
    private ArrayList<String> getData() {
        updateData();
        return list;
    }
    private void updateData() {
        list.add("180平米的房子");
        list.add("一个勤劳漂亮的老婆");
        list.add("一辆宝马");
        list.add("一个强壮且永不生病的身体");
        list.add("一个喜欢的事业");
    }


    class MyListAdapter extends BaseAdapter {
        String[] data = new String[]{"领券受众","发票助手","收获地址","身边国美","我的收藏","我的评价","我的设备", "客服小妹"};
        @Override
        public int getCount() {
            return data.length;
        }

        @Override
        public Object getItem(int i) {
            return data[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                viewHolder = new ViewHolder();
                view = new RelativeLayout(getActivity());
                TextView textView = new TextView(getActivity());
                textView.setText(data[i]);
                ((RelativeLayout)view).addView(textView);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
                viewHolder.relativeLayout = (RelativeLayout) view;
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
                RelativeLayout relativeLayout = viewHolder.relativeLayout;
                TextView textView = (TextView) relativeLayout.getChildAt(0);
                textView.setText(data[i]);
                relativeLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
            }
            return view;
        }


        class ViewHolder {
            RelativeLayout relativeLayout;
        }
    }
}