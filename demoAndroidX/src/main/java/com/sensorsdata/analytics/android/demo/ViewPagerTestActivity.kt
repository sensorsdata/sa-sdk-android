/*
 * Created by zhangwei on 2019/04/17.
 * Copyright 2015－2019 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.demo

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import kotlinx.android.synthetic.main.activity_viewpager_fragment.*

class ViewPagerTestActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewpager_fragment)
        viewPager.adapter = TestPagerAdapter(supportFragmentManager)
    }

    class TestPagerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm) {
        private var fragmentList: List<Fragment> = listOf(Test1Fragment(), Test1Fragment(), Test1Fragment(), Test1Fragment())
        private var fragmentTitleList: List<String> = listOf("title1", "title2", "title3", "title4")
        override fun getItem(position: Int): Fragment = fragmentList[position]
        override fun getCount(): Int = fragmentList.size
        override fun getPageTitle(position: Int): CharSequence? = fragmentTitleList[position]
    }
}
