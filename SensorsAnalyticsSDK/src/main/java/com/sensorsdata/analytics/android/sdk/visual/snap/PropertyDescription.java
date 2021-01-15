/*
 * Created by wangzhuozhou on 2015/08/01.
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

package com.sensorsdata.analytics.android.sdk.visual.snap;

public class PropertyDescription {

    public final String name;
    public final Class<?> targetClass;
    public final Caller accessor;
    private final String mMutatorName;

    public PropertyDescription(String name, Class<?> targetClass, Caller accessor,
                               String mutatorName) {
        this.name = name;
        this.targetClass = targetClass;
        this.accessor = accessor;

        mMutatorName = mutatorName;
    }

    @Override
    public String toString() {
        return "[PropertyDescription " + name + "," + targetClass + ", " + accessor + "/" + mMutatorName
                + "]";
    }
}
