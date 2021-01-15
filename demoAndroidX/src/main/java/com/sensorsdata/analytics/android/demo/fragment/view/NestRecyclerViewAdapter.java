/*
 * Created by dengshiwei on 2020/09/28.
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

package com.sensorsdata.analytics.android.demo.fragment.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class NestRecyclerViewAdapter extends RecyclerView.Adapter<NestRecyclerViewAdapter.BaseHolder> {
    private Context context;
    private final int LIST_VIEW = 1000;
    private final int TEXT_VIEW = 1001;
    private List<String> datas = new ArrayList<String>();
    public NestRecyclerViewAdapter(Context context){
        this.context = context;
        for (int i = 0; i < 10; i++) {
            datas.add("测试数据：" + i);
        }
    }


    @NonNull
    @Override
    public BaseHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (LIST_VIEW == viewType) {
            ListView listView = new MyListView(context, null);
            return new ListViewHolder(listView);
        }
        return new TextViewHolder(new TextView(context));
    }

    @Override
    public void onBindViewHolder(@NonNull BaseHolder holder, int position) {
        if (holder instanceof ListViewHolder) {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_expandable_list_item_1,
                    datas);
            ((ListViewHolder) holder).listView.setAdapter(adapter);
        } else if(holder instanceof TextViewHolder) {
            ((TextViewHolder) holder).textView.setText(position + " 位置");
        }
    }

    @Override
    public int getItemCount() {
        return datas.size() + 20;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return LIST_VIEW;
        }
        return TEXT_VIEW;
    }

    public class BaseHolder extends RecyclerView.ViewHolder {
        public BaseHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public class ListViewHolder extends BaseHolder {
        public ListView listView;
        public ListViewHolder(@NonNull View itemView) {
            super(itemView);
            this.listView = (ListView) itemView;
            this.listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                }
            });
        }
    }

    public class TextViewHolder extends BaseHolder {
        public TextView textView;
        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
            this.textView = (TextView) itemView;
            this.textView.setMinHeight(30);
            this.textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }
}
