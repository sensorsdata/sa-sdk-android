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

import android.content.Intent
import android.os.Bundle
import com.sensorsdata.analytics.android.demo.R
import com.sensorsdata.analytics.android.sdk.PropertyBuilder
import com.sensorsdata.analytics.android.sdk.ScreenAutoTracker
import kotlinx.android.synthetic.main.activity_view_screen.*
import org.json.JSONObject

class ViewScreenActivity : BaseActivity(), ScreenAutoTracker {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_screen)

        btn.setOnClickListener {
            val intent = Intent(this@ViewScreenActivity, ViewScreenIgnoreActivity::class.java)
            startActivity(intent)
        }

    }

    override fun getScreenUrl(): String = "testMainActivity screen url test"

    override fun getTrackProperties(): JSONObject = PropertyBuilder.newInstance()
            .append("\$screen_name", "test screen name")
            .append("\$title", "test title ss")
            .append("other_property", "others").toJSONObject()

}