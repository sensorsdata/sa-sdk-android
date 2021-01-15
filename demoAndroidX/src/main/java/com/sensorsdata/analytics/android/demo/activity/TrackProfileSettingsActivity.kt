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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sensorsdata.analytics.android.demo.R
import com.sensorsdata.analytics.android.sdk.PropertyBuilder
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI
import com.sensorsdata.analytics.android.sdk.SensorsDataTrackViewOnClick
import kotlinx.android.synthetic.main.activity_track_profile.*
import java.util.*

class TrackProfileSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_profile)
        initView()
    }

    private fun initView() {
    }

    @SensorsDataTrackViewOnClick
    fun doOnClick(view: View) {
        when (view.id) {
            //产生一个自定义事件
            R.id.track_a_event -> {
                SensorsDataAPI.sharedInstance(this).track("ViewProduct",
                        PropertyBuilder.newInstance()
                                .append("ProductPrice", 100)
                                .append("ProductName", "Apple").toJSONObject())
            }
            R.id.track_installation -> {
                //check permission
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                    Manifest.permission.READ_PHONE_STATE)) {
                        val setIntent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null))
                        startActivityForResult(setIntent, 101)
                    } else {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 123)
                    }
                    return
                }
                SensorsDataAPI.sharedInstance(this).trackAppInstall(PropertyBuilder.newInstance().append("pKey", "pValue").toJSONObject())
            }
            //匿名 ID 和用户 ID 关联
            R.id.login_btn -> {
                SensorsDataAPI.sharedInstance(this).login("130xxxx1234",
                        PropertyBuilder.newInstance()
                                .append("grade", 4)
                                .append("school", "北大附小").toJSONObject())
            }
            //设置用户profile
            R.id.profile_set_btn -> {
                SensorsDataAPI.sharedInstance(this).profileSet(PropertyBuilder.newInstance()
                        .append("name", "this is username")
                        .append("schoolAddress", "this is an address")
                        .append("money", 100).toJSONObject())
            }
            //修改profile
            R.id.profile_increment_btn -> {
                SensorsDataAPI.sharedInstance(this).profileIncrement("money", 2)
            }
            R.id.profile_append -> {
                SensorsDataAPI.sharedInstance(this).profileAppend("likesport", "sport" + Random().nextInt(100))
            }
            R.id.profile_delete -> {
                SensorsDataAPI.sharedInstance().profileDelete()
            }
            R.id.item_set -> {
                SensorsDataAPI.sharedInstance().itemSet("itemType", "itemId", PropertyBuilder
                        .newInstance().append("item", "item").toJSONObject())
            }
            R.id.item_delete -> {
                SensorsDataAPI.sharedInstance().itemDelete("itemType", "itemId")
            }
            R.id.trackChannelEvent -> {
                SensorsDataAPI.sharedInstance().trackChannelEvent("hello_world211")
            }
            R.id.track_view_onclick -> {
                SensorsDataAPI.sharedInstance().trackViewAppClick(track_view_onclick)
            }
            R.id.track_view_screen -> {
                SensorsDataAPI.sharedInstance().trackViewScreen(this)
            }
            else -> {
                //no op
            }
        }
    }
}