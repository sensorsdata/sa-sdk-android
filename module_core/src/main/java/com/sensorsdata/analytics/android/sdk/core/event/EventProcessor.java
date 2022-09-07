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

package com.sensorsdata.analytics.android.sdk.core.event;

import com.sensorsdata.analytics.android.sdk.core.SAContextManager;
import com.sensorsdata.analytics.android.sdk.core.event.imp.AssembleDataImpl;
import com.sensorsdata.analytics.android.sdk.core.event.imp.SendDataImpl;
import com.sensorsdata.analytics.android.sdk.core.event.imp.StoreDataImpl;

/**
 * Event Processor
 */
public abstract class EventProcessor {
    IAssembleData mAssembleData;
    IStoreData mStoreData;
    ISendData mSendData;

    public EventProcessor(SAContextManager saContextManager) {
        mAssembleData = new AssembleDataImpl(saContextManager);
        mSendData = new SendDataImpl(saContextManager);
        mStoreData = new StoreDataImpl();
    }

    public abstract void trackEvent(InputData input);

    /**
     * data process
     *
     * @param input DataInput
     */
    protected synchronized void process(InputData input) {
        // 1. assemble data
        Event event = assembleData(input);
        // 2. store data
        int errorCode = storeData(event);
        // 3. send data
        sendData(input, errorCode);
    }

    /**
     * Assemble Track Event
     *
     * @return Event
     */
    Event assembleData(InputData input) {
        return mAssembleData.assembleData(input);
    }

    /**
     * store data into db
     *
     * @param event Event
     * @return ErrorCode
     */
    int storeData(Event event) {
        return mStoreData.storeData(event);
    }

    /**
     * send data to server
     */
    void sendData(InputData inputData, int code) {
        mSendData.sendData(inputData, code);
    }

    /* The interface is used to assemble data */
    public interface IAssembleData {
        Event assembleData(InputData input);
    }

    /* The interface is used to store data */
    public interface IStoreData {
        /**
         * ErrorCode
         *
         * @param event Event
         * @return ErrorCode
         */
        int storeData(Event event);
    }

    /* The interface is used to send data */
    public interface ISendData {
        /**
         * @param inputData InputData
         * @param code insert data ErrorCode
         */
        void sendData(InputData inputData, int code);
    }
}
