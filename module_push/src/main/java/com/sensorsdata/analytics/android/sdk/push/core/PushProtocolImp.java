/*
 * Created by dengshiwei on 2022/08/02.
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

package com.sensorsdata.analytics.android.sdk.push.core;

import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.push.SAPushProtocol;
import com.sensorsdata.analytics.android.sdk.monitor.SensorsDataLifecycleMonitorManager;

public class PushProtocolImp implements SAPushProtocol {
    private boolean mEnable = false;

    @Override
    public void install(SAContextManager contextManager) {
        if (contextManager.getInternalConfigs().saConfigOptions.isEnableTrackPush()) {
            SensorsDataLifecycleMonitorManager.getInstance().addActivityLifeCallback(new PushLifecycleCallbacks());
        }
        setModuleState(!contextManager.getInternalConfigs().saConfigOptions.isDisableSDK());
    }

    @Override
    public void setModuleState(boolean enable) {
        if (mEnable != enable) {
            mEnable = enable;
        }
    }

    @Override
    public String getModuleName() {
        return ModuleConstants.ModuleName.PUSH_NAME;
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
        return null;
    }
}
