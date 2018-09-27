/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk;

/**
 * SDK内部接口
 */
public interface ResourceIds {

    boolean knownIdName(String name);

    int idFromName(String name);

    String nameForId(int id);

}
