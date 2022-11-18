/*
 * Created by dengshiwei on 2022/07/11.
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

package com.sensorsdata.analytics.android.sdk.exposure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureConfig;
import com.sensorsdata.analytics.android.sdk.core.business.exposure.SAExposureData;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.SAModuleManager;
import com.sensorsdata.analytics.android.sdk.util.AppStateTools;
import com.sensorsdata.analytics.android.sdk.util.ReflectUtil;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.util.WeakHashMap;


@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class SensorsDataExposureTest {
    Application mApplication = ApplicationProvider.getApplicationContext();
    SensorsDataAPI mSensorsDataAPI = null;
    ExposureActivity mActivity = Robolectric.setupActivity(ExposureActivity.class);
    View mView;

    @Test
    public void testExposureHelper() {
        addExposureView();
        removeExposureView();
        setExposureIdentifier();
    }

    public SensorsDataAPI setUp() {
        mSensorsDataAPI = null;
        mSensorsDataAPI = SAHelper.initSensors(mApplication);
        mView = new View(mActivity.getBaseContext());
        return mSensorsDataAPI;
    }


    private void setExposureIdentifier() {
        setUp();
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mActivity.addContentView(mView, layoutParams);
        AppStateTools.getInstance().onActivityCreated(mActivity, null);

        String exposureIdentifier = "aaaa";
        mSensorsDataAPI.setExposureIdentifier(mView, exposureIdentifier);
        //1.获取当前的 ExposureView 看对应的信息是否匹配
        ExposureView exposureView = getExposureView();
        //校验 ExposureData 是否相同
        SAExposureData handleExposureData = exposureView.getExposureData();
        assertEquals(exposureIdentifier, handleExposureData.getIdentifier());
    }

    private void addExposureView() {
        setUp();
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mActivity.addContentView(mView, layoutParams);
        AppStateTools.getInstance().onActivityCreated(mActivity, null);

        SAExposureConfig exposureConfig = new SAExposureConfig(0.2f, 1, true);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("x-key", "x-value");
            jsonObject.put("y-key", "y-value");
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        SAExposureData exposureData = new SAExposureData("aaa", jsonObject, null, exposureConfig);
        mSensorsDataAPI.addExposureView(mView, exposureData);
        //1.获取当前的 ExposureView 看对应的信息是否匹配
        ExposureView exposureView = getExposureView();
        //校验 ExposureData 是否相同
        SAExposureData handleExposureData = exposureView.getExposureData();
        assertEquals(exposureData, handleExposureData);
    }

    private void removeExposureView() {
        addExposureView();
        mSensorsDataAPI.removeExposureView(mView, null);
        ExposureView exposureView = getExposureView();
        assertNull(exposureView);
    }

    private ExposureView getExposureView() {
        SAExposureProtocolImpl exposureProtocol = getSAExposureProtocolImpl();
        SAExposedProcess exposedProcess = ReflectUtil.findField(new String[]{exposureProtocol.getClass().getName()}, exposureProtocol, "mExposedProcess");
        WeakHashMap<Activity, ExposedPage> exposedPageWeakHashMap = ReflectUtil.findField(new String[]{exposedProcess.getClass().getName()}, exposedProcess, "mExposedPageWeakHashMap");
        ExposedPage exposedPage = exposedPageWeakHashMap.get(mActivity);
        ExposureView exposureView = exposedPage.getExposureView(mView);
        return exposureView;
    }

    public static class ExposureActivity extends Activity {
        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setTheme(android.R.style.Theme_Material);
        }
    }

    private SAExposureProtocolImpl getSAExposureProtocolImpl() {
        Class cls = SAModuleManager.getInstance().getClass();
        SAExposureProtocolImpl saExposureProtocol = null;
        try {
            Method method = cls.getDeclaredMethod("getService", new Class[]{String.class});
            method.setAccessible(true);
            saExposureProtocol = (SAExposureProtocolImpl) method.invoke(SAModuleManager.getInstance(), Modules.Exposure.MODULE_NAME);
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
        return saExposureProtocol;
    }
}