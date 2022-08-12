/*
 * Created by chenru on 2022/6/29 下午7:07.
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

package com.sensorsdata.analytics.android.sdk.advert.model;

/**
 * 落地页类型
 * INTELLIGENCE：智能落地页
 * OTHER：自定义落地页
 */
public enum SATLandingPageType {
    INTELLIGENCE("intelligence"),
    OTHER("other");

    private String mTypeName;

    SATLandingPageType(String typeName) {
        this.mTypeName = typeName;
    }

    public String getTypeName(){
        return mTypeName;
    }
}
