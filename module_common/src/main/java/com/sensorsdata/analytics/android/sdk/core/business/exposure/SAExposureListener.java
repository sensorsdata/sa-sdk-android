/*
 * Created by dengshiwei on 2023/04/17.
 * Copyright 2015－2023 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.core.business.exposure;

import android.view.View;

public interface SAExposureListener {
    /**
     * 返回对应 View 是否曝光
     *
     * @param view View
     * @param exposureData View 对应数据
     * @return true：曝光，false：不曝光
     */
    boolean shouldExposure(View view, SAExposureData exposureData);

    /**
     * 曝光完成回调
     *
     * @param view View
     * @param exposureData 曝光数据
     */
    void didExposure(View view, SAExposureData exposureData);
}
