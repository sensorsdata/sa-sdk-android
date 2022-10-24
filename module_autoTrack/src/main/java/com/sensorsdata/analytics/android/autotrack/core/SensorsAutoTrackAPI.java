/*
 * Created by dengshiwei on 2022/07/06.
 * Copyright 2015－2022 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.autotrack.core;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.Modules;
import com.sensorsdata.analytics.android.sdk.core.mediator.protocol.SAModuleProtocol;

public class SensorsAutoTrackAPI implements SAModuleProtocol {
    private AutoTrackContextHelper mAutoTrackHelper;
    private boolean mEnable = false;

    @Override
    public void install(SAContextManager contextManager) {
        try {
            mAutoTrackHelper = new AutoTrackContextHelper(contextManager);
            setModuleState(!contextManager.getInternalConfigs().saConfigOptions.isDisableSDK());
        } catch (Exception e) {
            SALog.printStackTrace(e);
        }
    }

    @Override
    public void setModuleState(boolean enable) {
        if (mEnable != enable) {
            mEnable = enable;
        }
    }

    @Override
    public String getModuleName() {
        return Modules.AutoTrack.MODULE_NAME;
    }

    @Override
    public boolean isEnable() {
        return mEnable;
    }

    @Override
    public int getPriority() {
        return 5;
    }

    @Override
    public <T> T invokeModuleFunction(String methodName, Object... argv) {
        return mAutoTrackHelper.invokeModuleFunction(methodName, argv);
    }
}
