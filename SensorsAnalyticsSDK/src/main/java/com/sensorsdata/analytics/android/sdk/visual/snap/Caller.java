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

import android.view.View;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Caller {

    private static final String TAG = "SA.Caller";
    private final String mMethodName;
    private final Object[] mMethodArgs;
    private final Class<?> mMethodResultType;
    private final Class<?> mTargetClass;
    private final Method mTargetMethod;

    public Caller(Class<?> targetClass, String methodName, Object[] methodArgs, Class<?> resultType)
            throws NoSuchMethodException {
        mMethodName = methodName;

        // TODO if this is a bitmap, we might be hogging a lot of memory here.
        // We likely need a caching/loading to disk layer for bitmap-valued edits
        // I'm going to kick this down the road for now.
        mMethodArgs = methodArgs;
        mMethodResultType = resultType;

        mTargetMethod = pickMethod(targetClass);
        if (null == mTargetMethod) {
            throw new NoSuchMethodException(
                    "Method " + targetClass.getName() + "." + mMethodName + " doesn't exit");
        }

        mTargetClass = mTargetMethod.getDeclaringClass();
    }

    private static Class<?> assignableArgType(Class<?> type) {
        // a.isAssignableFrom(b) only tests if b is a
        // subclass of a. It does not handle the autoboxing case,
        // i.e. when a is an int and b is an Integer, so we have
        // to make the Object types primitive types. When the
        // function is finally invoked, autoboxing will take
        // care of the the cast.
        if (type == Byte.class) {
            type = byte.class;
        } else if (type == Short.class) {
            type = short.class;
        } else if (type == Integer.class) {
            type = int.class;
        } else if (type == Long.class) {
            type = long.class;
        } else if (type == Float.class) {
            type = float.class;
        } else if (type == Double.class) {
            type = double.class;
        } else if (type == Boolean.class) {
            type = boolean.class;
        } else if (type == Character.class) {
            type = char.class;
        }

        return type;
    }

    @Override
    public String toString() {
        return "[Caller " + mMethodName + "(" + mMethodArgs + ")" + "]";
    }

    public Object[] getArgs() {
        return mMethodArgs;
    }

    public Object applyMethod(View target) {
        return applyMethodWithArguments(target, mMethodArgs);
    }

    public Object applyMethodWithArguments(View target, Object[] arguments) {
        final Class<?> klass = target.getClass();
        if (mTargetClass.isAssignableFrom(klass)) {
            try {
                return mTargetMethod.invoke(target, arguments);
            } catch (final IllegalAccessException e) {
                SALog.i(TAG, "Method " + mTargetMethod.getName() + " appears not to be public", e);
            } catch (final IllegalArgumentException e) {
                SALog.i(TAG,
                        "Method " + mTargetMethod.getName() + " called with arguments of the wrong type", e);
            } catch (final InvocationTargetException e) {
                SALog.i(TAG, "Method " + mTargetMethod.getName() + " threw an exception", e);
            }
        }

        return null;
    }

    public boolean argsAreApplicable(Object[] proposedArgs) {
        final Class<?>[] paramTypes = mTargetMethod.getParameterTypes();
        if (proposedArgs.length != paramTypes.length) {
            return false;
        }

        for (int i = 0; i < proposedArgs.length; i++) {
            final Class<?> paramType = assignableArgType(paramTypes[i]);
            if (null == proposedArgs[i]) {
                if (paramType == byte.class ||
                        paramType == short.class ||
                        paramType == int.class ||
                        paramType == long.class ||
                        paramType == float.class ||
                        paramType == double.class ||
                        paramType == boolean.class ||
                        paramType == char.class) {
                    return false;
                }
            } else {
                final Class<?> argumentType = assignableArgType(proposedArgs[i].getClass());
                if (!paramType.isAssignableFrom(argumentType)) {
                    return false;
                }
            }
        }

        return true;
    }

    private Method pickMethod(Class<?> klass) {
        final Class<?>[] argumentTypes = new Class[mMethodArgs.length];
        for (int i = 0; i < mMethodArgs.length; i++) {
            argumentTypes[i] = mMethodArgs[i].getClass();
        }

        for (final Method method : klass.getMethods()) {
            final String foundName = method.getName();
            final Class<?>[] params = method.getParameterTypes();

            if (!foundName.equals(mMethodName) || params.length != mMethodArgs.length) {
                continue;
            }

            final Class<?> assignType = assignableArgType(mMethodResultType);
            final Class<?> resultType = assignableArgType(method.getReturnType());
            if (!assignType.isAssignableFrom(resultType)) {
                continue;
            }

            boolean assignable = true;
            for (int i = 0; i < params.length && assignable; i++) {
                final Class<?> argumentType = assignableArgType(argumentTypes[i]);
                final Class<?> paramType = assignableArgType(params[i]);
                assignable = paramType.isAssignableFrom(argumentType);
            }

            if (!assignable) {
                continue;
            }

            return method;
        }

        return null;
    }
}
