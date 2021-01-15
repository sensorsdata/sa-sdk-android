/*
 * Created by zhangwei on 2019/04/17.
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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sensorsdata.analytics.android.demo.PopupMenuActivity
import com.sensorsdata.analytics.android.demo.R
import com.sensorsdata.analytics.android.demo.custom.HorizonRecyclerDivider
import com.sensorsdata.analytics.android.sdk.util.SensorsDataUtils
import kotlinx.android.synthetic.main.activity_test_list.*

class TestMainActivity : AppCompatActivity() {
    private lateinit var testListAdapter: TestMainAdapter
    private lateinit var dataList: List<DataEntity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_list)
        dataList = listOf(DataEntity("通用 Property 和基础接口设置", BasePropertyActivity::class.java, true),
                DataEntity("track*, profile* 等接口调用", TrackProfileSettingsActivity::class.java, true),
                DataEntity("OnClick", ClickActivity::class.java, true),
                DataEntity("H5 页面测试", H5Activity::class.java, true),
                DataEntity("可视化内嵌 H5", H5VisualTestActivity::class.java, true),
                DataEntity("Widget 采集测试", WidgetTestActivity::class.java, true),
                DataEntity("ViewPager & Fragment 测试", FragmentActivity::class.java, true),
                DataEntity("TabHost", MyTabHostActivity::class.java, true),
                DataEntity("NavigationView", NavigationViewActivity::class.java, true),
                DataEntity("ViewScreen", ViewScreenActivity::class.java, true),
                DataEntity("ListView & ExpandableListView", ListViewTestActivity::class.java, true),
                DataEntity("GridView ", GridViewTestActivity::class.java, true),
                DataEntity("hint 采集", HintTestActivity::class.java, true),
                DataEntity("Crash 测试", CrashTestActivity::class.java, true),
                DataEntity("PopupMenu 测试", PopupMenuActivity::class.java, true),
                DataEntity("Dialog", DialogActivity::class.java, true),
                DataEntity("黑名单白名单", BaseActivity::class.java, false),
                DataEntity("Debug 模式", BaseActivity::class.java, false),
                DataEntity("点击图 HeatMap", BaseActivity::class.java),
                DataEntity("可视化全埋点", BaseActivity::class.java),
                DataEntity("ListView 内嵌", InnerListTestActivity::class.java, true),
                DataEntity("ActionBar && ToolBar", ActionBarAndToolBarTestActivity::class.java, true),
                DataEntity("Lambda 点击事件", LambdaTestPageActivity::class.java, true)
        )
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.addItemDecoration(HorizonRecyclerDivider(this, HorizonRecyclerDivider.VERTICAL_LIST))
        testListAdapter = TestMainAdapter(this, dataList)
        recyclerView.adapter = testListAdapter
    }

    class DataEntity(val content: String, val activityClazz: Class<*>, val isSupported: Boolean = false)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        SensorsDataUtils.handleSchemeUrl(this, intent)
    }
}