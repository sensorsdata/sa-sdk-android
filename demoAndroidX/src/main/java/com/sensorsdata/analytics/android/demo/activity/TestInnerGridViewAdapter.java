/*
 * Created by zhangxiangwei on 2020/02/26.
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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sensorsdata.analytics.android.demo.R;

import java.util.ArrayList;
import java.util.List;


public class TestInnerGridViewAdapter extends RecyclerView.Adapter<TestInnerGridViewAdapter.ViewHolder> {

    private Context context;
    private List<String> data;

    public TestInnerGridViewAdapter(Context context, List<String> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.activity_test_gridview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        List list = new ArrayList();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }
        holder.gridView.setAdapter(new MyAdapter(list));
        holder.gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private GridView gridView;

        public ViewHolder(View itemView) {
            super(itemView);
            gridView = itemView.findViewById(R.id.gv);
        }
    }

    public class MyAdapter extends BaseAdapter {

        private List list;

        public MyAdapter(List list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GridViewHolder holder = null;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(
                        R.layout.item_test_gridview, null);
                holder = new GridViewHolder();
                holder.textView = (TextView) convertView
                        .findViewById(R.id.grid_tv);
                convertView.setTag(holder);

            } else {
                holder = (GridViewHolder) convertView.getTag();
            }
            holder.textView.setText(list.get(position).toString());

            return convertView;
        }

        class GridViewHolder {
            TextView textView;
        }
    }


}