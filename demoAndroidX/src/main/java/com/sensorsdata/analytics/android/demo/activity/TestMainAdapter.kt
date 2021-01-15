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

package com.sensorsdata.analytics.android.demo.activity

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.sensorsdata.analytics.android.demo.R
import kotlinx.android.synthetic.main.item_test.view.*

class TestMainAdapter(private val context: Context, private val dataList: List<TestMainActivity.DataEntity>) : RecyclerView.Adapter<TestMainAdapter.TestListViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestListViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_test, parent, false)
        return TestListViewHolder(view)
    }

    override fun getItemCount(): Int = dataList.size

    override fun onBindViewHolder(holder: TestListViewHolder, position: Int) {
        holder.itemView.contentTV.text = "${String.format("%02d", position + 1)}、${dataList[position].content}"
        holder.itemView.setOnClickListener {
            if (dataList[position].isSupported) {
                val intent = Intent(context, dataList[position].activityClazz)
                intent.putExtra("title", dataList[position].content)
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Not Added Now", Toast.LENGTH_LONG).show()
            }
        }
    }

    class TestListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}