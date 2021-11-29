/*
 * Created by zhangxiangwei on 2020/03/19.
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

package com.sensorsdata.analytics.android.sdk.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ReflectUtil {

    static <T> T findField(Class<?> clazz, Object instance, String... fieldName) {
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
                currentClass = Class.forName(className[i]);
            } catch (Exception e) {
                currentClass = null;
            }
            if (currentClass != null) {
                break;
            }
        }
        return currentClass;
    }

    public static boolean isInstance(Object object, String... args) {
        if (args == null || args.length == 0) {
            return false;
        }
        Class clazz = null;
        boolean result = false;
        for (String arg : args) {
            try {
                clazz = Class.forName(arg);
                result = clazz.isInstance(object);
            } catch (Exception e) {
                //ignored
            }
            if (result) {
                break;
            }
        }
        return result;
    }

    public static <T> T callMethod(Object instance, String methodName, Object... args) {
        Class[] argsClass = new Class[args.length];
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
        Class[] argsClass = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            argsClass[i] = args[i].getClass();
        }
        if (clazz != null) {
            Method method = getMethod(clazz, methodName, argsClass);
            if (method != null) {
                try {
                    return (T) method.invoke(null, args);
                } catch (Exception e) {
                    // Ignored
                }
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
            if(field == null){
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

    static <T> T findFieldRecur(Object instance, String fieldName) {
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
