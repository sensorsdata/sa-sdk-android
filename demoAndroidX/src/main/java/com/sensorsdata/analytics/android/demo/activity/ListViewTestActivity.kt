/*
 * Created by chenru on 2019/06/20.
 * Copyright 2015－2020 Sensors Data Inc.
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

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.sensorsdata.analytics.android.demo.R
import kotlinx.android.synthetic.main.activity_list.*

class ListViewTestActivity : BaseActivity() {
    private val groups = arrayOf("A", "B", "C")
    private val childs = arrayOf(arrayOf("A1", "A2", "A3", "A4"), arrayOf("B1", "B2", "B3", "B4"), arrayOf("C1", "C2", "C3", "C4"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        expandableListView.setAdapter(MyExpandableAdapter())
        listView.setOnItemClickListener { _, _, _, _ ->
            println("list view item clicked")
        }
        expandableListView.setOnGroupClickListener { _, _, _, _ ->
            println("group item clicked")
            false
        }
        expandableListView.setOnChildClickListener { _, _, _, _, _ ->
            println("child item clicked")
            true
        }
    }

    internal inner class MyExpandableAdapter : BaseExpandableListAdapter() {

        override fun getGroupCount(): Int {
            return groups.size
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            return childs[groupPosition].size
        }

        override fun getGroup(groupPosition: Int): Any {
            return groups[groupPosition]
        }

        override fun getChild(groupPosition: Int, childPosition: Int): Any {
            return childs[groupPosition][childPosition]
        }

        override fun getGroupId(groupPosition: Int): Long {
            return groupPosition.toLong()
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return childPosition.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
            var convertViewRef = convertView
            if (convertViewRef == null) {
                convertViewRef = layoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
            }
            val textView = convertViewRef as TextView
            textView.text = groups[groupPosition]
            textView.setBackgroundColor(Color.GRAY)
            return convertViewRef
        }

        override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
            var convertViewRef = convertView
            if (convertViewRef == null) {
                convertViewRef = layoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
            }
            val textView = convertViewRef as TextView
            textView.text = childs[groupPosition][childPosition]
            return convertViewRef
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return true
        }
    }
}