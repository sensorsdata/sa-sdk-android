/*
 * Created by chenru on 2019/4/3.
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

import android.text.TextUtils;

import com.sensorsdata.analytics.android.sdk.SALog;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;


/**
 * 线程安全的日期格式化工具类
 * create on 2019/4/3
 *
 * @author : chenru
 */
public class TimeUtils {
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static Locale SDK_LOCALE = Locale.CHINA;
    private static final String YYYY_MM_DD_HH_MM_SS_SSS = "yyyy-MM-dd HH:mm:ss.SSS";
    private static Map<String, ThreadLocal<SimpleDateFormat>> formatMaps = new HashMap<>();

    /**
     * format Date 输出文本格式
     * patten 默认使用 YYYY_MM_DD_HH_MM_SS_SSS
     * 例：2019-04-12 11:22:00.408
     * Locale 默认使用 Default
     *
     * @param timeMillis 时间戳
     * @return 日期展示字符串
     */
    public static String formatTime(long timeMillis) {
        return formatTime(timeMillis, SDK_LOCALE);
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
        return formatTime(timeMillis, patten, SDK_LOCALE);
    }

    /**
     * format Date 输出文本格式
     * patten 默认使用 YYYY_MM_DD_HH_MM_SS_SSS
     * 例：2019-04-12 11:22:00.408
     * Locale 默认使用 Default
     *
     * @param timeMillis 时间戳
     * @param locale Locale
     * @return 日期展示字符串
     */
    public static String formatTime(long timeMillis, Locale locale) {
        return formatTime(timeMillis, null, locale);
    }

    /**
     * format Date 输出文本格式
     * patten 默认使用 YYYY_MM_DD_HH_MM_SS_SSS
     * 例：2019-04-12 11:22:00.408
     * Locale 默认使用 Default
     *
     * @param timeMillis 时间戳
     * @param patten 时间展示模板
     * @param locale Locale
     * @return 日期展示字符串
     */
    public static String formatTime(long timeMillis, String patten, Locale locale) {
        String formatString = "";
        if (TextUtils.isEmpty(patten)) {
            patten = YYYY_MM_DD_HH_MM_SS_SSS;
        }
        SimpleDateFormat simpleDateFormat = getDateFormat(patten, locale);
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
        return formatDate(date, patten, SDK_LOCALE);
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

    /**
     * 验证日期是否合法
     *
     * @param date Date
     * @return 是否合法
     */
    public static boolean isDateValid(Date date) {
        try {
            SimpleDateFormat simpleDateFormat = getDateFormat(YYYY_MM_DD_HH_MM_SS_SSS, SDK_LOCALE);
            final Date baseDate = simpleDateFormat.parse("2015-05-15 10:24:00.000");
            return date.after(baseDate);
        } catch (ParseException e) {
            SALog.printStackTrace(e);
        }

        return false;
    }

    /**
     * 验证日期是否合法，目前校验比较粗糙，仅要求数据在 "2015-05-15 10:24:00.000" 以后
     *
     * @param time Time
     * @return 是否合法
     */
    public static boolean isDateValid(long time) {
        try {
            SimpleDateFormat simpleDateFormat = getDateFormat(YYYY_MM_DD_HH_MM_SS_SSS, SDK_LOCALE);
            final Date baseDate = simpleDateFormat.parse("2015-05-15 10:24:00.000");
            if (baseDate == null) {
                return false;
            }
            return baseDate.getTime() < time;
        } catch (ParseException e) {
            SALog.printStackTrace(e);
        }

        return false;
    }

    /**
     * 将 JSONObject 中的 Date 类型数据格式化
     *
     * @param jsonObject JSONObject
     * @return JSONObject
     */
    public static JSONObject formatDate(JSONObject jsonObject) {
        if (jsonObject == null) {
            return new JSONObject();
        }
        try {
            Iterator<String> iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                Object value = jsonObject.get(key);
                if (value instanceof Date) {
                    jsonObject.put(key, formatDate((Date) value, SDK_LOCALE));
                }
            }
        } catch (JSONException e) {
            SALog.printStackTrace(e);
        }
        return jsonObject;
    }

    /**
     * 获取时区偏移值
     *
     * @return 时区偏移值，单位：分钟
     */
    public static Integer getZoneOffset() {
        try {
            Calendar cal = Calendar.getInstance(Locale.getDefault());
            int zoneOffset = cal.get(java.util.Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
            return -zoneOffset / (1000 * 60);
        } catch (Exception ex) {
            SALog.printStackTrace(ex);
        }
        return null;
    }

    /**
     * 计算间隔时长，单位秒
     *
     * @param startTime 启动时间
     * @param endTime 退出时间
     * @return 时长
     */
    public static Float duration(long startTime, long endTime) {
        long duration = endTime - startTime;
        try {
            if (duration < 0 || duration > 24 * 60 * 60 * 1000) {
                return Float.valueOf(0);
            }
            return Float.valueOf(Math.round(duration) / 1000.0F);
        } catch (Exception e) {
            SALog.printStackTrace(e);
            return Float.valueOf(0);
        }
    }

    /**
     * 获取保留 3 位的时长
     *
     * @param duration Duration，单位毫秒
     * @return 时长
     */
    public static Float duration(float duration) {
        return Float.valueOf(Math.round(duration) / 1000.0F);
    }

    private synchronized static SimpleDateFormat getDateFormat(final String patten, final Locale locale) {
        ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = formatMaps.get(patten + "_" + (locale == null ? SDK_LOCALE.getCountry() : locale.getCountry()));
        if (null == dateFormatThreadLocal) {
            dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {
                @Override
                protected SimpleDateFormat initialValue() {
                    SimpleDateFormat simpleDateFormat = null;
                    try {
                        if (locale == null) {
                            simpleDateFormat = new SimpleDateFormat(patten, TimeUtils.SDK_LOCALE);
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
                formatMaps.put(patten + "_" + (locale == null ? SDK_LOCALE.getCountry() : locale.getCountry()), dateFormatThreadLocal);
            }
        }
        return dateFormatThreadLocal.get();
    }
}
