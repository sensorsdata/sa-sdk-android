/*
 * Created by wangzhuozhou on 2017/5/5.
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

package com.sensorsdata.analytics.android.sdk;

import android.util.Log;


public class SALog {
    private static boolean debug;
    private static boolean enableLog;
    private static boolean disableSDK;
    private static final int CHUNK_SIZE = 4000;

    public static void d(String tag, String msg) {
        if (debug && !disableSDK) {
            info(tag, msg, null);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (debug && !disableSDK) {
            info(tag, msg, tr);
        }

    }

    public static void i(String tag, String msg) {
        if (enableLog && !disableSDK) {
            info(tag, msg, null);
        }
    }

    public static void i(String tag, Throwable tr) {
        if (enableLog && !disableSDK) {
            info(tag, "", tr);
        }
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (enableLog && !disableSDK) {
            info(tag, msg, tr);
        }
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableLog 会修改此方法
     *
     * @param tag String
     * @param msg String
     * @param tr Throwable
     */
    public static void info(String tag, String msg, Throwable tr) {
        try {
            if (msg != null) {
                byte[] bytes = msg.getBytes();
                int length = bytes.length;
                if (length <= CHUNK_SIZE) {
                    Log.i(tag, msg, tr);
                } else {
                    int index = 0, lastIndexOfLF = 0;
                    //当最后一次剩余值小于 CHUNK_SIZE 时，不需要再截断
                    while (index < length - CHUNK_SIZE) {
                        lastIndexOfLF = lastIndexOfLF(bytes, index);
                        int chunkLength = lastIndexOfLF - index;
                        Log.i(tag, new String(bytes, index, chunkLength), null);
                        if (chunkLength < CHUNK_SIZE) {
                            //跳过换行符
                            index = lastIndexOfLF + 1;
                        } else {
                            index = lastIndexOfLF;
                        }
                    }
                    if (length > index) {
                        Log.i(tag, new String(bytes, index, length - index), tr);
                    }
                }
            } else {
                Log.i(tag, null, tr);
            }
        } catch (Exception e) {
            printStackTrace(e);
        }
    }

    /**
     * 获取从 fromIndex 开始，最靠近尾部的换行符
     *
     * @param bytes 日志转化的 bytes 数组
     * @param fromIndex 从 bytes 开始的下标
     * @return 换行符的下标
     */
    private static int lastIndexOfLF(byte[] bytes, int fromIndex) {
        int index = Math.min(fromIndex + CHUNK_SIZE, bytes.length - 1);
        for (int i = index; i > index - CHUNK_SIZE; i--) {
            //返回换行符的位置
            if (bytes[i] == (byte) 10) {
                return i;
            }
        }
        return index;
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableLog 会修改此方法
     *
     * @param e Exception
     */
    public static void printStackTrace(Exception e) {
        if (enableLog && !disableSDK && e != null) {
            Log.e("SA.Exception", "", e);
        }
    }

    /**
     * 设置 Debug 状态
     *
     * @param isDebug Debug 状态
     */
    static void setDebug(boolean isDebug) {
        debug = isDebug;
    }

    /**
     * 设置是否打印 Log
     *
     * @param isEnableLog Log 状态
     */
    public static void setEnableLog(boolean isEnableLog) {
        enableLog = isEnableLog;
    }

    public static void setDisableSDK(boolean configDisableSDK) {
        disableSDK = configDisableSDK;
    }

    public static boolean isLogEnabled() {
        return enableLog;
    }
}
