/*
 * Created by dengshiwei on 2019/12/25.
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

import android.content.Context;
import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;

public class OaidHelper {
    private static final String TAG = "SA.DeviceUtils";
    // OAID
    private static String mOAID = "";
    private static CountDownLatch mCountDownLatch;
    private static Class<?> mIdentifyListener;
    private static Class<?> mIdSupplier;
    private static Class<?> jLibrary;
    private static Class<?> mMidSDKHelper;
    
    /**
     * 获取 OAID 接口，注意该接口是同步接口，可能会导致线程阻塞，建议在子线程中使用
     *
     * @param context Context
     * @return OAID
     */
    public static String getOAID(final Context context) {
        try {
            mCountDownLatch = new CountDownLatch(1);
            initInvokeListener();
            if (mMidSDKHelper == null || mIdentifyListener == null || mIdSupplier == null) {
                SALog.d(TAG, "OAID 读取类创建失败");
                return "";
            }
            if (TextUtils.isEmpty(mOAID)) {
                getOAIDReflect(context, 2);
            } else {
                return mOAID;
            }
            try {
                mCountDownLatch.await();
            } catch (InterruptedException e) {
                SALog.printStackTrace(e);
            }
            SALog.d(TAG, "CountDownLatch await");
            return mOAID;
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return "";
    }

    /**
     * 通过反射获取 OAID，结果返回的 ErrorCode 如下：
     * 1008611：不支持的设备厂商
     * 1008612：不支持的设备
     * 1008613：加载配置文件出错
     * 1008614：获取接口是异步的，结果会在回调中返回，回调执行的回调可能在工作线程
     * 1008615：反射调用出错
     *
     * @param context Context
     * @param retryCount 重试次数
     */
    private static void getOAIDReflect(Context context, int retryCount) {
        try {
            if (retryCount == 0) {
                return;
            }
            final int INIT_ERROR_RESULT_DELAY = 1008614;            //获取接口是异步的，结果会在回调中返回，回调执行的回调可能在工作线程
            // 初始化 Library
            if (jLibrary != null) {
                Method initEntry = jLibrary.getDeclaredMethod("InitEntry", Context.class);
                initEntry.invoke(null, context);
            }
            IdentifyListenerHandler handler = new IdentifyListenerHandler();
            Method initSDK = mMidSDKHelper.getDeclaredMethod("InitSdk", Context.class, boolean.class, mIdentifyListener);
            int errCode = (int) initSDK.invoke(null, context, true, Proxy.newProxyInstance(context.getClassLoader(), new Class[]{mIdentifyListener}, handler));
            SALog.d(TAG, "MdidSdkHelper ErrorCode : " + errCode);
            if (errCode != INIT_ERROR_RESULT_DELAY) {
                getOAIDReflect(context, --retryCount);
                if (retryCount == 0) {
                    mCountDownLatch.countDown();
                }
            }

            /*
             * 此处是为了适配三星部分手机，根据 MSA 工作人员反馈，对于三星部分机型的支持有 bug，导致
             * 返回 1008614 错误码，但是不会触发回调。所以此处的逻辑是，两秒之后主动放弃阻塞。
             */
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                    mCountDownLatch.countDown();
                }
            }).start();
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
            getOAIDReflect(context, --retryCount);
            if (retryCount == 0) {
                mCountDownLatch.countDown();
            }
        }
    }

    static class IdentifyListenerHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                if ("OnSupport".equals(method.getName())) {
                    if ((Boolean) args[0]) {
                        Method getOAID = mIdSupplier.getDeclaredMethod("getOAID");
                        mOAID = (String) getOAID.invoke(args[1]);
                        SALog.d(TAG, "oaid:" + mOAID);
                    }

                    mCountDownLatch.countDown();
                }
            } catch (Exception ex) {
                mCountDownLatch.countDown();
            }
            return null;
        }
    }

    private static void initInvokeListener() {
        try {
            mMidSDKHelper = Class.forName("com.bun.miitmdid.core.MdidSdkHelper");
        } catch (ClassNotFoundException e) {
            SALog.printStackTrace(e);
            return;
        }
        // 尝试 1.0.22 版本
        try {
            mIdentifyListener = Class.forName("com.bun.miitmdid.interfaces.IIdentifierListener");
            mIdSupplier = Class.forName("com.bun.miitmdid.interfaces.IdSupplier");
            return;
        } catch (Exception ex) {
            // ignore
        }

        // 尝试 1.0.13 - 1.0.21 版本
        try {
            mIdentifyListener = Class.forName("com.bun.supplier.IIdentifierListener");
            mIdSupplier = Class.forName("com.bun.supplier.IdSupplier");
            jLibrary = Class.forName("com.bun.miitmdid.core.JLibrary");
            return;
        } catch (Exception ex) {
            // ignore
        }

        // 尝试 1.0.5 - 1.0.13 版本
        try {
            mIdentifyListener = Class.forName("com.bun.miitmdid.core.IIdentifierListener");
            mIdSupplier = Class.forName("com.bun.miitmdid.supplier.IdSupplier");
            jLibrary = Class.forName("com.bun.miitmdid.core.JLibrary");
        } catch (Exception ex) {
            // ignore
        }
    }
}