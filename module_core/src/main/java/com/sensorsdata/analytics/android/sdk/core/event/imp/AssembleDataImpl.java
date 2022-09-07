/*
 * Created by dengshiwei on 2022/06/14.
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

package com.sensorsdata.analytics.android.sdk.core.event.imp;

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.internal.beans.EventType;
import com.sensorsdata.analytics.android.sdk.core.event.Event;
import com.sensorsdata.analytics.android.sdk.core.event.EventProcessor;
import com.sensorsdata.analytics.android.sdk.core.event.InputData;

public class AssembleDataImpl implements EventProcessor.IAssembleData {
    private final TrackEventAssemble mTrackEventAssemble;
    private final ItemEventAssemble mItemEventAssemble;
    private final H5TrackAssemble mH5EventAssemble;

    public AssembleDataImpl(SAContextManager saContextManager) {
        mTrackEventAssemble = new TrackEventAssemble(saContextManager);
        mItemEventAssemble = new ItemEventAssemble(saContextManager);
        mH5EventAssemble = new H5TrackAssemble(saContextManager);
    }

    @Override
    public Event assembleData(InputData input) {
        if (!TextUtils.isEmpty(input.getExtras())) {
            return mH5EventAssemble.assembleData(input);
        } else if (input.getEventType() == EventType.ITEM_DELETE || input.getEventType() == EventType.ITEM_SET){
            return mItemEventAssemble.assembleData(input);
        } else {
           return mTrackEventAssemble.assembleData(input);
        }
    }
}
