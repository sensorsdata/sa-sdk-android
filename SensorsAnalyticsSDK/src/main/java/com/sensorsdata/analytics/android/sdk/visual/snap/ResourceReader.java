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

import android.content.Context;
import android.util.SparseArray;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * SDK内部接口
 */
public abstract class ResourceReader implements ResourceIds {

    @SuppressWarnings("unused")
    private static final String TAG = "SA.ResourceReader";
    private final Context mContext;
    private final Map<String, Integer> mIdNameToId;
    private final SparseArray<String> mIdToIdName;

    protected ResourceReader(Context context) {
        mContext = context;
        mIdNameToId = new HashMap<String, Integer>();
        mIdToIdName = new SparseArray<String>();
    }

    private static void readClassIds(Class<?> platformIdClass, String namespace,
                                     Map<String, Integer> namesToIds) {
        try {
            final Field[] fields = platformIdClass.getFields();
            for (final Field field : fields) {
                final int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    final Class fieldType = field.getType();
                    if (fieldType == int.class) {
                        final String name = field.getName();
                        final int value = field.getInt(null);
                        final String namespacedName;
                        if (null == namespace) {
                            namespacedName = name;
                        } else {
                            namespacedName = namespace + ":" + name;
                        }

                        namesToIds.put(namespacedName, value);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            SALog.i(TAG, "Can't read built-in id names from " + platformIdClass.getName(), e);
        }
    }

    @Override
    public boolean knownIdName(String name) {
        return mIdNameToId.containsKey(name);
    }

    @Override
    public int idFromName(String name) {
        return mIdNameToId.get(name);
    }

    @Override
    public String nameForId(int id) {
        return mIdToIdName.get(id);
    }

    protected abstract Class<?> getSystemClass();

    protected abstract String getLocalClassName(Context context);

    protected void initialize() {
        mIdNameToId.clear();
        mIdToIdName.clear();

        final Class<?> sysIdClass = getSystemClass();
        readClassIds(sysIdClass, "android", mIdNameToId);

        final String localClassName = getLocalClassName(mContext);
        try {
            final Class<?> rIdClass = Class.forName(localClassName);
            readClassIds(rIdClass, null, mIdNameToId);
        } catch (ClassNotFoundException e) {
            SALog.i(TAG, "Can't load names for Android view ids from '" + localClassName
                    + "', ids by name will not be available in the events editor.");
            SALog.i(TAG,
                    "You may be missing a Resources class for your package due to your proguard configuration, "
                            + "or you may be using an applicationId in your build that isn't the same as the "
                            + "package declared in your AndroidManifest.xml file.\n"
                            + "If you're using proguard, you can fix this issue by adding the following to your"
                            + " proguard configuration:\n\n"
                            + ""
                            + "-keep class **.R$* {\n"
                            + "    <fields>;\n"
                            + "}\n\n"
                            + ""
                            + "If you're not using proguard, or if your proguard configuration already contains"
                            + " the directive above, you can add the following to your AndroidManifest.xml file"
                            + " to explicitly point the SensorsData library to the appropriate library for your"
                            + " resources class:\n\n"
                            + "<meta-data android:name=\"com.sensorsdata.analytics.android.ResourcePackageName\""
                            + " android:value=\"YOUR_PACKAGE_NAME\" />\n\n"
                            + "where YOUR_PACKAGE_NAME is the same string you use for the \"package\" attribute"
                            + " in your <manifest> tag.");
        }

        for (Map.Entry<String, Integer> idMapping : mIdNameToId.entrySet()) {
            mIdToIdName.put(idMapping.getValue(), idMapping.getKey());
        }
    }

    public static class Ids extends ResourceReader {

        private final String mResourcePackageName;

        public Ids(String resourcePackageName, Context context) {
            super(context);
            mResourcePackageName = resourcePackageName;
            initialize();
        }

        @Override
        protected Class<?> getSystemClass() {
            return android.R.id.class;
        }

        @Override
        protected String getLocalClassName(Context context) {
            return mResourcePackageName + ".R$id";
        }
    }

    @SuppressWarnings("unused")
    public static class Drawables extends ResourceReader {

        private final String mResourcePackageName;

        protected Drawables(String resourcePackageName, Context context) {
            super(context);
            mResourcePackageName = resourcePackageName;
            initialize();
        }

        @Override
        protected Class<?> getSystemClass() {
            return android.R.drawable.class;
        }

        @Override
        protected String getLocalClassName(Context context) {
            return mResourcePackageName + ".R$drawable";
        }
    }
}
