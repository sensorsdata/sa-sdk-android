/*
 * Created by dengshiwei on 2022/10/27.
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

package com.sensorsdata.analytics.android.sdk.core.business.exposure;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class SAExposureConfigTest {

    @Test
    public void getAreaRate() {
        SAExposureConfig saExposureConfig = new SAExposureConfig(1,1,true);
        assertEquals(1, saExposureConfig.getAreaRate(), 0.5);
    }

    @Test
    public void setAreaRate() {
        SAExposureConfig saExposureConfig = new SAExposureConfig(1,1,true);
        saExposureConfig.setAreaRate(2);
        assertEquals(2, saExposureConfig.getAreaRate(), 0.5);
    }

    @Test
    public void isRepeated() {
        SAExposureConfig saExposureConfig = new SAExposureConfig(1,1,true);
        saExposureConfig.setAreaRate(2);
        assertTrue(saExposureConfig.isRepeated());
    }

    @Test
    public void setRepeated() {
        SAExposureConfig saExposureConfig = new SAExposureConfig(1,1,true);
        saExposureConfig.setRepeated(false);
        assertFalse(saExposureConfig.isRepeated());
    }

    @Test
    public void getStayDuration() {
        SAExposureConfig saExposureConfig = new SAExposureConfig(1,1,true);
        assertEquals(1, saExposureConfig.getStayDuration(), 0.2);
    }

    @Test
    public void setStayDuration() {
        SAExposureConfig saExposureConfig = new SAExposureConfig(1,1,true);
        saExposureConfig.setStayDuration(2);
        assertEquals(2, saExposureConfig.getStayDuration(), 0.2);
    }

    @Test
    public void getDelayTime() {
        SAExposureConfig saExposureConfig = new SAExposureConfig(1,1,true);
        assertEquals(1, saExposureConfig.getStayDuration(), 0.2);
    }

    @Test
    public void setDelayTime() {
        SAExposureConfig saExposureConfig = new SAExposureConfig(1,1,true);
        saExposureConfig.setDelayTime(2);
        assertEquals(2, saExposureConfig.getDelayTime(), 0.2);
    }
}