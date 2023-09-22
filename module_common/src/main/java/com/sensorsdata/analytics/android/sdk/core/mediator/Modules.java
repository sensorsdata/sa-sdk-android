/*
 * Created by chenru on 2022/3/9 下午6:37.
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

package com.sensorsdata.analytics.android.sdk.core.mediator;

public interface Modules {
    interface Advert {
        String MODULE_NAME = "sensors_analytics_module_advertisement";
        String METHOD_TRACK_INSTALLATION = "trackInstallation";
        String METHOD_TRACK_DEEPLINK_LAUNCH = "trackDeepLinkLaunch";
        String METHOD_TRACK_CHANNEL_EVENT = "trackChannelEvent";
        String METHOD_ENABLE_DEEPLINK_INSTALL_SOURCE = "enableDeepLinkInstallSource";
        String METHOD_SET_DEEPLINK_CALLBACK = "setDeepLinkCallback";
        String METHOD_SET_DEEPLINK_COMPLETION = "setDeepLinkCompletion";
        String METHOD_REQUEST_DEFERRED_DEEPLINK = "requestDeferredDeepLink";
        String METHOD_SEND_EVENT_SAT = "sendEvent";
        String METHOD_MERGE_CHANNEL_EVENT_PROPERTIES = "mergeChannelEventProperties";
        String METHOD_GET_LATEST_UTM_PROPERTIES = "getLatestUtmProperties";
        String METHOD_REMOVE_DEEPLINK_INFO = "removeDeepLinkInfo";
        String METHOD_COMMIT_REQUEST_DEFERRED_DEEPLINK = "commitRequestDeferredDeeplink";
        String METHOD_HANDLER_SCAN_URI = "handlerScanUri";
    }

    interface Visual {
        String MODULE_NAME = "sensors_analytics_module_visual";
        String METHOD_REQUEST_VISUAL_CONFIG = "requestVisualConfig";
        String METHOD_MERGE_VISUAL_PROPERTIES = "mergeVisualProperties";
        String METHOD_RESUME_VISUAL_SERVICE = "resumeVisualService";
        String METHOD_STOP_VISUAL_SERVICE = "stopVisualService";
        String METHOD_ADD_VISUAL_JAVASCRIPTINTERFACE = "addVisualJavascriptInterface";
        String METHOD_RESUME_HEATMAP_SERVICE = "resumeHeatMapService";
        String METHOD_STOP_HEATMAP_SERVICE = "stopHeatMapService";
        String METHOD_SHOW_PAIRING_CODE_INPUTDIALOG = "showPairingCodeInputDialog";
        String METHOD_SHOW_OPEN_HEATMAP_DIALOG = "showOpenHeatMapDialog";
        String METHOD_SHOW_OPEN_VISUALIZED_AUTOTRACK_DIALOG = "showOpenVisualizedAutoTrackDialog";
        String METHOD_H5_GET_APPVISUAL_CONFIG = "h5GetAppVisualConfig";
        String METHOD_FLUTTER_GET_APPVISUAL_CONFIG = "flutterGetAppVisualConfig";
        String METHOD_GET_VISUAL_STATE = "getVisualState";
        String METHOD_SEND_VISUALIZED_MESSAGE = "sendVisualizedMessage";
    }

    interface Push {
        String MODULE_NAME = "sensors_analytics_module_push";
    }

    interface AutoTrack {
        String MODULE_NAME = "sensors_analytics_module_autotrack";
        String METHOD_ENABLE_AUTO_TRACK = "enableAutoTrack";
        String METHOD_DISABLE_AUTO_TRACK = "disableAutoTrack";
        String METHOD_IS_AUTOTRACK_ENABLED = "isAutoTrackEnabled";
        String METHOD_IGNORE_AUTOTRACK_ACTIVITIES = "ignoreAutoTrackActivities";
        String METHOD_RESUME_AUTOTRACK_ACTIVITIES = "resumeAutoTrackActivities";
        String METHOD_IGNORE_AUTOTRACK_ACTIVITY = "ignoreAutoTrackActivity";
        String METHOD_RESUME_AUTOTRACK_ACTIVITY = "resumeAutoTrackActivity";
        String METHOD_IGNORE_AUTOTRACK_FRAGMENTS = "ignoreAutoTrackFragments";
        String METHOD_IGNORE_AUTOTRACK_FRAGMENT = "ignoreAutoTrackFragment";
        String METHOD_RESUME_IGNORED_AUTOTRACK_FRAGMENTS = "resumeIgnoredAutoTrackFragments";
        String METHOD_RESUME_IGNORED_AUTOTRACK_FRAGMENT = "resumeIgnoredAutoTrackFragment";
        String METHOD_IS_ACTIVITY_AUTOTRACK_APPVIEWSCREEN_IGNORED = "isActivityAutoTrackAppViewScreenIgnored";
        String METHOD_IS_ACTIVITY_AUTOTRACK_APPCLICK_IGNORED = "isActivityAutoTrackAppClickIgnored";
        String METHOD_IS_TRACK_FRAGMENT_APPVIEWSCREEN_ENABLED = "isTrackFragmentAppViewScreenEnabled";
        String METHOD_IS_AUTOTRACK_EVENT_TYPE_IGNORED = "isAutoTrackEventTypeIgnored";
        String METHOD_IS_FRAGMENT_AUTOTRACK_APPVIEWSCREEN = "isFragmentAutoTrackAppViewScreen";
        String METHOD_SET_VIEW_ID = "setViewID";
        String METHOD_SET_VIEW_ACTIVITY = "setViewActivity";
        String METHOD_SET_VIEW_FRAGMENT_NAME = "setViewFragmentName";
        String METHOD_TRACK_FRAGMENT_APPVIEWSCREEN = "trackFragmentAppViewScreen";
        String METHOD_ENABLE_AUTOTRACK_FRAGMENT = "enableAutoTrackFragment";
        String METHOD_ENABLE_AUTOTRACK_FRAGMENTS = "enableAutoTrackFragments";
        String METHOD_IGNORE_VIEW = "ignoreView";
        String METHOD_SET_VIEW_PROPERTIES = "setViewProperties";
        String METHOD_GET_IGNORED_VIEW_TYPE_LIST = "getIgnoredViewTypeList";
        String METHOD_IGNORE_VIEW_TYPE = "ignoreViewType";
        String METHOD_GET_LAST_SCREENURL = "getLastScreenUrl";
        String METHOD_CLEAR_REFERRER_WHEN_APPEND = "clearReferrerWhenAppEnd";
        String METHOD_GET_LAST_SCREEN_TRACK_PROPERTIES = "getLastScreenTrackProperties";
        String METHOD_CLEAR_LAST_SCREENURL = "clearLastScreenUrl";
        String METHOD_TRACK_VIEW_SCREEN = "trackViewScreen";
        String METHOD_TRACK_VIEW_APPCLICK = "trackViewAppClick";
        String METHOD_GET_REFERRER_SCREEN_TITLE = "getReferrerScreenTitle";
    }

    interface Encrypt {
        String MODULE_NAME = "sensors_analytics_module_encrypt";
        String METHOD_ENCRYPT_AES = "encryptAES";
        String METHOD_DECRYPT_AES = "decryptAES";
        String METHOD_VERIFY_SECRET_KEY = "verifySecretKey";
        String METHOD_ENCRYPT_EVENT_DATA_WITH_KEY = "encryptEventDataWithKey";
        String METHOD_ENCRYPT_EVENT_DATA = "encryptEventData";
        String METHOD_STORE_SECRET_KEY = "storeSecretKey";
        String METHOD_LOAD_SECRET_KEY = "loadSecretKey";
        String METHOD_STORE_EVENT = "storeEvent";
        String METHOD_LOAD_EVENT = "loadEvent";
        String METHOD_VERIFY_SUPPORT_TRANSPORT = "supportTransportEncrypt";
    }

    interface WebView {
        String MODULE_NAME = "sensors_analytics_module_webview";
        String METHOD_SHOWUP_WEBVIEW = "showUpWebView";
        String METHOD_SHOWUP_X5WEBVIEW = "showUpX5WebView";
    }

    interface Exposure {
        String MODULE_NAME = "sensors_analytics_module_exposure";
        String METHOD_SET_EXPOSURE_IDENTIFIER = "setExposureIdentifier";
        String METHOD_ADD_EXPOSURE_VIEW = "addExposureView";
        String METHOD_REMOVE_EXPOSURE_VIEW = "removeExposureView";
        String METHOD_UPDATE_EXPOSURE_PROPERTIES = "updateExposureProperties";
    }
}
