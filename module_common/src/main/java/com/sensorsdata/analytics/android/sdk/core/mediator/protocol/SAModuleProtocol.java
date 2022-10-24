/*
 * Created by chenru on 2022/3/16 下午2:04.
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

package com.sensorsdata.analytics.android.sdk.core.mediator.protocol;

import com.sensorsdata.analytics.android.sdk.core.SAContextManager;

public interface SAModuleProtocol {
    void install(SAContextManager contextManager);

    void setModuleState(boolean enable);

    String getModuleName();

    boolean isEnable();

    // module init priority, scope [1..10]
    int getPriority();

    /**
     * invoke module protocol api
     *
     * @param methodName Method Name
     * @param argv Method Arguments value
     * @param <T> return Type
     * @return T
     */
    <T> T invokeModuleFunction(String methodName, Object... argv);
}
