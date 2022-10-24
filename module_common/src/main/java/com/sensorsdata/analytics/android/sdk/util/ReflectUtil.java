/*
 * Created by zhangxiangwei on 2020/03/19.
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

package com.sensorsdata.analytics.android.sdk.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.LruCache;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class ReflectUtil {

    @SuppressLint("NewApi")
    private static final LruCache<String, Class<?>> mObjectLruCache = new LruCache<>(64);
    private static final Set<String> mObjectSet = new HashSet<String>();

    public static <T> T findField(Class<?> clazz, Object instance, String... fieldName) {
        T t = null;
        Field field = findFieldObj(clazz, fieldName);
        if (field == null) {
            return t;
        }
        try {
            return (T) field.get(instance);
        } catch (IllegalAccessException e) {
            return t;
        } catch (Exception e) {
            return t;
        }
    }

    public static <T> T findField(String[] className, Object instance, String... fieldName) {
        Class<?> currentClass = getCurrentClass(className);
        if (currentClass != null) {
            return findField(currentClass, instance, fieldName);
        }
        return null;
    }

    public static Class<?> getCurrentClass(String[] className) {
        if (className == null || className.length == 0) {
            return null;
        }
        Class<?> currentClass = null;
        for (int i = 0; i < className.length; i++) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    currentClass = mObjectLruCache.get(className[i]);
                }
                if (currentClass == null && !mObjectSet.contains(className[i])) {
                    currentClass = Class.forName(className[i]);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                        mObjectLruCache.put(className[i], currentClass);
                    }
                }
            } catch (Throwable e) {
                currentClass = null;
                mObjectSet.add(className[i]);
            }
            if (currentClass != null) {
                break;
            }
        }
        return currentClass;
    }

    public static Class<?> getClassByName(String name) {
        Class<?> compatClass = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                compatClass = mObjectLruCache.get(name);
            }
            if (compatClass == null && !mObjectSet.contains(name)) {
                compatClass = Class.forName(name);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    mObjectLruCache.put(name, compatClass);
                }
            }

        } catch (ClassNotFoundException e) {
            mObjectSet.add(name);
            return null;
        } catch (Throwable e) {
            return null;
        }
        return compatClass;
    }

    public static boolean isInstance(Object object, String... args) {
        if (args == null || args.length == 0) {
            return false;
        }
        Class clazz = null;
        boolean result = false;
        for (String arg : args) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                    clazz = mObjectLruCache.get(arg);
                }
                if (clazz == null && !mObjectSet.contains(arg)) {
                    clazz = Class.forName(arg);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                        mObjectLruCache.put(arg, clazz);
                    }
                }
                if (clazz != null) {
                    result = clazz.isInstance(object);
                }
            } catch (Throwable e) {
                mObjectSet.add(arg);
            }
            if (result) {
                break;
            }
        }
        return result;
    }

    public static <T> T callMethod(Object instance, String methodName, Object... args) {
        Class<?>[] argsClass = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            argsClass[i] = args[i].getClass();
        }
        Method method = getMethod(instance.getClass(), methodName, argsClass);
        if (method != null) {
            try {
                return (T) method.invoke(instance, args);
            } catch (Exception e) {
                // Ignored
            }
        }
        return null;
    }

    public static <T> T callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        if (clazz == null) {
            return null;
        }
        Class<?>[] argsClass = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            argsClass[i] = args[i].getClass();
        }
        Method method = getMethod(clazz, methodName, argsClass);
        if (method != null) {
            try {
                return (T) method.invoke(null, args);
            } catch (Exception e) {
                // Ignored
            }
        }
        return null;
    }

    static Method getDeclaredRecur(Class<?> clazz, String methodName, Class<?>... params) {
        while (clazz != Object.class) {
            try {
                Method method = clazz.getDeclaredMethod(methodName, params);
                if (method != null) {
                    return method;
                }
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    static Method getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        try {
            return clazz.getMethod(methodName, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    static Field findFieldObj(Class<?> clazz, String... fieldName) {
        try {
            if (fieldName == null || fieldName.length == 0) {
                return null;
            }
            Field field = null;
            for (int i = 0; i < fieldName.length; i++) {
                try {
                    field = clazz.getDeclaredField(fieldName[i]);
                } catch (NoSuchFieldException ex) {
                    field = null;
                }
                if (field != null) {
                    break;
                }
            }
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            return null;
        }
    }

    static Field findFieldObjRecur(Class<?> current, String fieldName) {
        while (current != Object.class) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

   public static <T> T findFieldRecur(Object instance, String fieldName) {
        Field field = findFieldObjRecur(instance.getClass(), fieldName);
        if (field != null) {
            try {
                return (T) field.get(instance);
            } catch (IllegalAccessException e) {
            }
        }
        return null;
    }
}
