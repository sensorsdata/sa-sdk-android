/*
 * Created by zhangxiangwei on 2020/07/09.
 * Copyright 2015－2020 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.visual.view;


interface IPairingCodeInterface {

    /**
     * 设置位数
     */
    void setFigures(int figures);

    /**
     * 设置配对码之间的间距
     */
    void setPairingCodeMargin(int margin);

    /**
     * 设置底部选中状态的颜色
     */
    void setBottomSelectedColor(int bottomSelectedColor);

    /**
     * 设置底部未选中状态的颜色
     */
    void setBottomNormalColor(int bottomNormalColor);

    /**
     * 设置选择的背景色
     */
    void setSelectedBackgroundColor(int selectedBackground);

    /**
     * 设置底线的高度
     */
    void setBottomLineHeight(int bottomLineHeight);

    /**
     * 设置当配对码变化时候的监听器
     */
    void setOnPairingCodeChangedListener(OnPairingCodeChangedListener listener);

    /**
     * 配对码变化时候的监听事件
     */
    interface OnPairingCodeChangedListener {

        /**
         * 当配对码变化的时候
         */
        void onPairingCodeChanged(CharSequence s, int start, int before, int count);

        /**
         * 输入完毕后的回调
         */
        void onInputCompleted(CharSequence s);
    }
}
