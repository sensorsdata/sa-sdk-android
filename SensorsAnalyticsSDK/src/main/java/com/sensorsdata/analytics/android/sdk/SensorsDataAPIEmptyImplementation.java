/*
 * Created by wangzhuozhou on 2015/08/01.
 * Copyright 2015－2020 Sensors Data Inc.
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


import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;

public class SensorsDataAPIEmptyImplementation extends SensorsDataAPI {
    SensorsDataAPIEmptyImplementation() {

    }

    @Override
    public JSONObject getPresetProperties() {
        return new JSONObject();
    }

    @Override
    public void enableAutoTrackFragment(Class<?> fragment) {
    }

    @Override
    public void enableAutoTrackFragments(List<Class<?>> fragmentsList) {
    }

    @Override
    public void ignoreAutoTrackFragments(List<Class<?>> fragmentList) {

    }

    @Override
    public void ignoreAutoTrackFragment(Class<?> fragment) {

    }

    @Override
    public void resumeIgnoredAutoTrackFragments(List<Class<?>> fragmentList) {

    }

    @Override
    public void resumeIgnoredAutoTrackFragment(Class<?> fragment) {

    }

    @Override
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        return false;
    }

    @Override
    public void setServerUrl(String serverUrl) {

    }

    @Override
    public void enableLog(boolean enable) {

    }

    @Override
    public boolean isDebugMode() {
        return false;
    }

    @Override
    public long getMaxCacheSize() {
        return 0;
    }

    @Override
    public void setMaxCacheSize(long maxCacheSize) {

    }

    @Override
    public void setFlushNetworkPolicy(int networkType) {

    }

    @Override
    public int getFlushInterval() {
        return 0;
    }

    @Override
    public void setFlushInterval(int flushInterval) {

    }

    @Override
    public int getFlushBulkSize() {
        return 0;
    }

    @Override
    public void setFlushBulkSize(int flushBulkSize) {

    }

    @Override
    public int getSessionIntervalTime() {
        return 30 * 1000;
    }

    @Override
    public void setSessionIntervalTime(int sessionIntervalTime) {
    }

    @Deprecated
    @Override
    public void enableAutoTrack() {

    }

    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {

    }

    @Override
    public void disableAutoTrack(List<AutoTrackEventType> eventTypeList) {

    }

    @Override
    public void disableAutoTrack(SensorsDataAPI.AutoTrackEventType autoTrackEventType) {

    }

    @Override
    public void trackAppCrash() {

    }

    @Override
    public boolean isAutoTrackEnabled() {
        return false;
    }

    @Override
    public void trackFragmentAppViewScreen() {

    }

    @Override
    public boolean isTrackFragmentAppViewScreenEnabled() {
        return false;
    }

    @Override
    public void enableReactNativeAutoTrack() {

    }

    @Override
    public boolean isReactNativeAutoTrackEnabled() {
        return false;
    }

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean) {

    }

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, boolean enableVerify) {

    }

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    @Deprecated
    public void showUpWebView(WebView webView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {

    }

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    @Deprecated
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, JSONObject properties) {

    }

    @Override
    @Deprecated
    public void showUpX5WebView(Object x5WebView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {

    }

    @Override
    public void showUpX5WebView(Object x5WebView, boolean enableVerify) {

    }

    @Override
    public void showUpX5WebView(Object x5WebView) {

    }

    @Override
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {

    }

    @Override
    public void resumeAutoTrackActivities(List<Class<?>> activitiesList) {

    }

    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {

    }

    @Override
    public void resumeAutoTrackActivity(Class<?> activity) {

    }

    @Override
    public boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity) {
        return true;
    }

    @Override
    public boolean isActivityAutoTrackAppClickIgnored(Class<?> activity) {
        return true;
    }

    @Deprecated
    @Override
    public void ignoreAutoTrackEventType(SensorsDataAPI.AutoTrackEventType autoTrackEventType) {

    }

    @Deprecated
    @Override
    public void ignoreAutoTrackEventType(List<AutoTrackEventType> eventTypeList) {

    }

    @Override
    public boolean isAutoTrackEventTypeIgnored(SensorsDataAPI.AutoTrackEventType eventType) {
        return true;
    }

    @Override
    public void setViewID(View view, String viewID) {

    }

    @Override
    public void setViewID(android.app.Dialog view, String viewID) {

    }

    @Override
    public void setViewID(Object view, String viewID) {

    }

    @Override
    public void setViewActivity(View view, Activity activity) {

    }

    @Override
    public void setViewFragmentName(View view, String fragmentName) {

    }

    @Override
    public void ignoreView(View view) {

    }

    @Override
    public void ignoreView(View view, boolean ignore) {

    }

    @Override
    public void setViewProperties(View view, JSONObject properties) {

    }

    @Override
    public List<Class> getIgnoredViewTypeList() {
        return new ArrayList<>();
    }

    @Override
    public void ignoreViewType(Class viewType) {

    }

    @Override
    public boolean isHeatMapActivity(Class<?> activity) {
        return false;
    }

    @Override
    public void addHeatMapActivity(Class<?> activity) {

    }

    @Override
    public void addHeatMapActivities(List<Class<?>> activitiesList) {

    }

    @Override
    public boolean isHeatMapEnabled() {
        return false;
    }

    @Override
    boolean isAppHeatMapConfirmDialogEnabled() {
        return true;
    }

    @Override
    public void enableAppHeatMapConfirmDialog(boolean enable) {

    }

    @Override
    public void enableHeatMap() {

    }

    @Override
    public String getDistinctId() {
        return null;
    }

    @Override
    public String getAnonymousId() {
        return null;
    }

    @Override
    public void resetAnonymousId() {

    }

    @Override
    public String getLoginId() {
        return null;
    }

    @Override
    public void identify(String distinctId) {

    }

    @Override
    public void login(String loginId) {

    }

    @Override
    public void login(String loginId, JSONObject properties) {

    }

    @Override
    public void logout() {

    }

    @Deprecated
    @Override
    public void trackSignUp(String newDistinctId, JSONObject properties) {

    }

    @Deprecated
    @Override
    public void trackSignUp(String newDistinctId) {

    }

    @Override
    public void trackInstallation(String eventName, JSONObject properties, boolean disableCallback) {

    }

    @Override
    public void trackInstallation(String eventName, JSONObject properties) {

    }

    @Override
    public void trackInstallation(String eventName) {

    }

    @Override
    public void trackChannelEvent(String eventName) {

    }

    @Override
    public void trackChannelEvent(final String eventName, JSONObject properties) {

    }

    @Override
    public void track(String eventName, JSONObject properties) {

    }

    @Override
    public void track(String eventName) {

    }

    void trackInternal(final String eventName, final JSONObject properties) {

    }

    @Deprecated
    @Override
    public void trackTimer(final String eventName) {

    }

    @Deprecated
    @Override
    public void trackTimer(final String eventName, final TimeUnit timeUnit) {

    }

    @Override
    public void removeTimer(String eventName) {
        super.removeTimer(eventName);
    }

    @Override
    public String trackTimerStart(String eventName) {
        return "";
    }

    @Override
    public void trackTimerBegin(final String eventName) {

    }

    @Override
    public void trackTimerBegin(final String eventName, final TimeUnit timeUnit) {

    }

    @Override
    public void trackTimerEnd(final String eventName, JSONObject properties) {

    }

    @Override
    public void trackTimerEnd(final String eventName) {

    }

    @Override
    public void clearTrackTimer() {

    }

    @Override
    public String getLastScreenUrl() {
        return null;
    }

    @Override
    public void clearReferrerWhenAppEnd() {

    }

    @Override
    public void clearLastScreenUrl() {

    }

    @Override
    public String getMainProcessName() {
        return "";
    }

    @Override
    public JSONObject getLastScreenTrackProperties() {
        return new JSONObject();
    }

    @Override
    public void trackViewScreen(String url, JSONObject properties) {

    }

    @Override
    public void trackViewScreen(Activity activity) {

    }

    @Override
    public void trackViewScreen(Object fragment) {

    }

    @Override
    public void trackViewAppClick(View view) {

    }

    @Override
    public void trackViewAppClick(View view, JSONObject properties) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void flushSync() {

    }

    @Override
    public void registerDynamicSuperProperties(SensorsDataDynamicSuperProperties dynamicSuperProperties) {

    }

    @Override
    public void setTrackEventCallBack(SensorsDataTrackEventCallBack trackEventCallBack) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public JSONObject getSuperProperties() {
        return new JSONObject();
    }

    @Override
    public void registerSuperProperties(JSONObject superProperties) {

    }

    @Override
    public void unregisterSuperProperty(String superPropertyName) {

    }

    @Override
    public void clearSuperProperties() {

    }

    @Override
    public void profileSet(JSONObject properties) {

    }

    @Override
    public void profileSet(String property, Object value) {

    }

    @Override
    public void profileSetOnce(JSONObject properties) {

    }

    @Override
    public void profileSetOnce(String property, Object value) {

    }

    @Override
    public void profileIncrement(Map<String, ? extends Number> properties) {

    }

    @Override
    public void profileIncrement(String property, Number value) {

    }

    @Override
    public void profileAppend(String property, String value) {

    }

    @Override
    public void profileAppend(String property, Set<String> values) {

    }

    @Override
    public void profileUnset(String property) {

    }

    @Override
    public void profileDelete() {

    }

    @Override
    public void trackEventFromH5(String eventInfo, boolean enableVerify) {

    }

    @Override
    public void trackEventFromH5(String eventInfo) {

    }

    @Override
    public void setGPSLocation(double latitude, double longitude) {

    }

    @Override
    public void clearGPSLocation() {

    }

    @Override
    public void enableTrackScreenOrientation(boolean enable) {

    }

    @Override
    public void resumeTrackScreenOrientation() {

    }

    @Override
    public void stopTrackScreenOrientation() {

    }

    @Override
    public void setCookie(String cookie, boolean encode) {

    }

    @Override
    public String getCookie(boolean decode) {
        return null;
    }

    @Override
    public void profilePushId(String pushTypeKey, String pushId) {

    }

    @Override
    public void profileUnsetPushId(String pushTypeKey) {

    }

    @Override
    public boolean isVisualizedAutoTrackActivity(Class<?> activity) {
        return false;
    }

    @Override
    public void addVisualizedAutoTrackActivity(Class<?> activity) {

    }

    @Override
    public void addVisualizedAutoTrackActivities(List<Class<?>> activitiesList) {

    }

    @Override
    public boolean isVisualizedAutoTrackEnabled() {
        return false;
    }

    @Override
    boolean isVisualizedAutoTrackConfirmDialogEnabled() {
        return false;
    }

    @Override
    public void enableVisualizedAutoTrackConfirmDialog(boolean enable) {

    }

    @Override
    public void enableVisualizedAutoTrack() {

    }

    @Override
    public void setSSLSocketFactory(SSLSocketFactory sf) {

    }

    @Override
    public void itemSet(String itemType, String itemId, JSONObject properties) {
    }

    @Override
    public void itemDelete(String itemType, String itemId) {
    }

    @Override
    public void enableNetworkRequest(boolean isRequest) {

    }

    @Override
    public void startTrackThread() {

    }

    @Override
    public void stopTrackThread() {

    }
}