/*
 * Created by chenru on 2019/4/3.
 * Copyright 2015－2019 Sensors Data Inc.
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

import android.text.TextUtils;
import com.sensorsdata.analytics.android.sdk.SALog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * 线程安全的日期格式化工具类
 * create on 2019/4/3
 * @author : chenru
 */
public class DateFormatUtils {
    private static Map<String, ThreadLocal<SimpleDateFormat>> formatMaps = new HashMap<>();
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String YYYY_MM_DD_HH_MM_SS_SSS = "yyyy-MM-dd HH:mm:ss.SSS";

    private synchronized static SimpleDateFormat getDateFormat(final String patten, final Locale locale) {
        ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = formatMaps.get(patten);
        if (null == dateFormatThreadLocal) {
            dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    SimpleDateFormat simpleDateFormat = null;
                    try {
                        if (locale == null) {
                            simpleDateFormat = new SimpleDateFormat(patten, Locale.getDefault());
                        } else {
                            simpleDateFormat = new SimpleDateFormat(patten, locale);
                        }
                    } catch (Exception e) {
                        SALog.printStackTrace(e);
                    }
                    return simpleDateFormat;
                }
            };
            if (null != dateFormatThreadLocal.get()) {
                formatMaps.put(patten, dateFormatThreadLocal);
            }
        }
        return dateFormatThreadLocal.get();
    }

    /**
     * format Date 输出文本格式
     * patten 默认使用 YYYY_MM_DD_HH_MM_SS_SSS
     * 例：2019-04-12 11:22:00.408
     * Locale 默认使用 Default
     *
     * @param timeMillis 时间戳
     * @param patten 时间展示模板
     * @return 日期展示字符串
     */
    public static String formatTime(long timeMillis, String patten) {
        String formatString = "";
        if (TextUtils.isEmpty(patten)) {
            patten = YYYY_MM_DD_HH_MM_SS_SSS;
        }
        SimpleDateFormat simpleDateFormat = getDateFormat(patten, Locale.getDefault());
        if (null == simpleDateFormat) {
            return formatString;
        }
        try {
            formatString = simpleDateFormat.format(timeMillis);
        } catch (IllegalArgumentException e) {
            SALog.printStackTrace(e);
        }
        return formatString;
    }

    /**
     * format Date 输出文本格式
     * patten 默认使用 YYYY_MM_DD_HH_MM_SS_SSS
     * 例：2019-04-12 11:22:00.408
     * Locale 默认使用 Default
     *
     * @param date 日期
     * @return 日期展示字符串
     */
    public static String formatDate(Date date) {
        return formatDate(date, YYYY_MM_DD_HH_MM_SS_SSS);
    }

    /**
     * format Date 输出文本格式
     * Locale 默认使用 Default
     *
     * @param date 日期
     * @param patten 时间展示模板
     * @return 日期展示字符串
     */
    public static String formatDate(Date date, String patten) {
        return formatDate(date, patten, Locale.getDefault());
    }

    /**
     * format Date 输出文本格式
     * patten 默认使用 YYYY_MM_DD_HH_MM_SS_SSS
     * 例：2019-04-12 11:22:00.408
     *
     * @param date 日期
     * @param locale 位置
     * @return 日期展示字符串
     */
    public static String formatDate(Date date, Locale locale) {
        return formatDate(date, YYYY_MM_DD_HH_MM_SS_SSS, locale);
    }

    /**
     * format Date 输出文本格式
     *
     * @param date 日期
     * @param patten 时间展示模板
     * @param locale 位置
     * @return 日期展示字符串
     */
    public static String formatDate(Date date, String patten, Locale locale) {
        if (TextUtils.isEmpty(patten)) {
            patten = YYYY_MM_DD_HH_MM_SS_SSS;
        }
        String formatString = "";
        SimpleDateFormat simpleDateFormat = getDateFormat(patten, locale);
        if (null == simpleDateFormat) {
            return formatString;
        }
        try {
            formatString = simpleDateFormat.format(date);
        } catch (IllegalArgumentException e) {
            SALog.printStackTrace(e);
        }
        return formatString;
    }
}
