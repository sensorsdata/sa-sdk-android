/*
 * Created by dengshiwei on 2022/06/30.
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

package com.sensorsdata.analytics.android.sdk.core.business.timer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.OLDEST_SDK})
public class EventTimerManagerTest {
    EventTimerManager mInstance;

    @Before
    public void setUp() {
        mInstance = EventTimerManager.getInstance();
    }

    @Test
    public void getInstance() {
        mInstance = EventTimerManager.getInstance();
        Assert.assertNotNull(mInstance);
    }

    @Test
    public void addEventTimer() {
        mInstance.addEventTimer("EventTimer", new EventTimer(TimeUnit.SECONDS, 10000L));
        Assert.assertNotNull(mInstance.getEventTimer("EventTimer"));
    }

    @Test
    public void removeTimer() {
        mInstance.addEventTimer("EventTimer", new EventTimer(TimeUnit.SECONDS, 10000L));
        mInstance.removeTimer("EventTimer");
        Assert.assertNull(mInstance.getEventTimer("EventTimer"));
    }

    @Test
    public void updateEndTime() {
        mInstance.addEventTimer("EventTimer", new EventTimer(TimeUnit.SECONDS, 10000L));
        mInstance.updateEndTime("EventTimer", 20000);
        Assert.assertEquals(20000, mInstance.getEventTimer("EventTimer").getEndTime());
    }

    @Test
    public void updateTimerState() {
        mInstance.addEventTimer("EventTimer", new EventTimer(TimeUnit.SECONDS, 10000L));
        mInstance.updateTimerState("EventTimer", 10000L,false);
        Assert.assertNotNull(mInstance.getEventTimer("EventTimer"));
    }

    @Test
    public void getEventTimer() {
        mInstance.addEventTimer("EventTimer", new EventTimer(TimeUnit.SECONDS, 10000L));
        Assert.assertNotNull(mInstance.getEventTimer("EventTimer"));
    }

    @Test
    public void clearTimers() {
        mInstance.addEventTimer("EventTimer", new EventTimer(TimeUnit.SECONDS, 10000L));
        mInstance.clearTimers();
        Assert.assertNull(mInstance.getEventTimer("EventTimer"));
    }

    @Test
    public void appBecomeActive() {
        mInstance.addEventTimer("EventTimer", new EventTimer(TimeUnit.SECONDS, 10000L));
        mInstance.appBecomeActive();
        Assert.assertEquals(100, mInstance.getEventTimer("EventTimer").getStartTime());
    }

    @Test
    public void appEnterBackground() {
        mInstance.addEventTimer("EventTimer", new EventTimer(TimeUnit.SECONDS, 10000L));
        mInstance.appEnterBackground();
        Assert.assertEquals(100, mInstance.getEventTimer("EventTimer").getStartTime());
    }
}