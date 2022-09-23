/*
 * Created by dengshiwei on 2022/08/10.
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

package com.sensorsdata.analytics.android.sdk.encrypt.impl;

import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.mediator.ModuleConstants;
import com.sensorsdata.analytics.android.sdk.core.mediator.encrypt.SAEncryptProtocol;

public class SAEncryptProtocolImpl implements SAEncryptProtocol {
    private SAEncryptAPIImpl mEncryptAPIImpl;
    private boolean mEnable = false;

    @Override
    public void install(SAContextManager contextManager) {
        mEncryptAPIImpl = new SAEncryptAPIImpl(contextManager);
        if (!contextManager.getInternalConfigs().saConfigOptions.isDisableSDK()) {
            setModuleState(true);
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
        return ModuleConstants.ModuleName.ENCRYPT_NAME;
    }

    @Override
    public boolean isEnable() {
        return mEnable;
    }

    @Override
    public int getPriority() {
        return 6;
    }

    @Override
    public <T> T invokeModuleFunction(String methodName, Object... argv) {
        return mEncryptAPIImpl.invokeModuleFunction(methodName, argv);
    }
}
