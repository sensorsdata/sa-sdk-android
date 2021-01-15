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

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.sensorsdata.analytics.android.demo.R
import kotlinx.android.synthetic.main.activity_list.*

class ListViewTestActivity : BaseActivity() {
    private val groups = arrayOf("A", "B", "C")
    private val childs = arrayOf(arrayOf("A1", "A2", "A3", "A4"), arrayOf("B1", "B2", "B3", "B4"), arrayOf("C1", "C2", "C3", "C4"))
    private var datas = ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        addELVHeadViewAndFootView()
        expandableListView.setAdapter(MyExpandableAdapter())
        for (i in 0..100) {
            datas.add(i.toString())
        }
        addHeadViewAndFootView();
        listView.adapter = MyListViewAdapter()
        listView.setOnItemClickListener { _, _, _, _ ->
            Toast.makeText(this@ListViewTestActivity, "list view item clicked", Toast.LENGTH_SHORT).show()
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

    internal inner class MyListViewAdapter : BaseAdapter() {

        var viewHolder: MyViewHolder? = null

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            if (convertView == null) {
                view = layoutInflater.inflate(R.layout.item_listview, null)
                viewHolder = MyViewHolder(view)
                view.tag = viewHolder
            } else {
                view = convertView
                viewHolder = convertView.tag as MyViewHolder
            }
            viewHolder!!.textView!!.text = datas[position]
            viewHolder!!.button1!!.text = "button 1： " + datas[position]
            viewHolder!!.textView!!.setOnClickListener {
                Toast.makeText(this@ListViewTestActivity, "textView: $position click", Toast.LENGTH_SHORT).show()
            }
            viewHolder!!.button1!!.setOnClickListener {
                Toast.makeText(this@ListViewTestActivity, "button1: $position click", Toast.LENGTH_SHORT).show()
            }
            viewHolder!!.button2!!.setOnClickListener {
                Toast.makeText(this@ListViewTestActivity, "button2: $position click", Toast.LENGTH_SHORT).show()
            }
            return view
        }

        override fun getItem(position: Int): Any {
            return datas[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return datas.size
        }

        inner class MyViewHolder(view: View) {
            var textView: TextView? = null
            var button1: Button? = null
            var button2: Button? = null


            init {
                textView = view.findViewById(R.id.textView);
                button1 = view.findViewById(R.id.button1);
                button2 = view.findViewById(R.id.button2);
            }

        }
    }

    private fun addHeadViewAndFootView() {
        var headView = layoutInflater.inflate(R.layout.item_test, null);
        listView.addHeaderView(headView);
        var footView = layoutInflater.inflate(R.layout.item_test, null);
        listView.addFooterView(footView);
    }

    private fun addELVHeadViewAndFootView() {
        var headView = layoutInflater.inflate(R.layout.item_test, null);
        headView.setOnClickListener {
            Toast.makeText(this@ListViewTestActivity, "headView: click", Toast.LENGTH_SHORT).show()
        }
        expandableListView.addHeaderView(headView);
        var footView = layoutInflater.inflate(R.layout.item_test, null);
        footView.setOnClickListener {
            Toast.makeText(this@ListViewTestActivity, "footView: click", Toast.LENGTH_SHORT).show()
        }
        expandableListView.addFooterView(footView);
    }

}