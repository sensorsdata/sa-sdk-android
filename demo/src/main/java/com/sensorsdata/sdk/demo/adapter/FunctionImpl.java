/*
 * Created by dengshiwei on 2025/02/17.
 * Copyright 2015－2023 Sensors Data Inc.
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

package com.sensorsdata.sdk.demo.adapter;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLSocketFactory;

public class FunctionImpl {

    public static void functionImp(String func) {
        switch (func) {
            case "login":
                SensorsDataAPI.sharedInstance().login("user_login");
                break;
            case "identify":
                SensorsDataAPI.sharedInstance().identify("user_identify");
                break;
            case "logout":
                SensorsDataAPI.sharedInstance().logout();
                break;
            case "profileSet":
                SensorsDataAPI.sharedInstance().profileSet("Adsource", "userSource");
                break;
            case "profileSetOnce":
                JSONObject newProperties = new JSONObject();
                try {
                    newProperties.put("AdSource", "Email");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // 再次设定用户渠道，设定无效，"developer@sensorsdata.cn" 的 "AdSource" 属性值仍然是 "XXX Store"
                SensorsDataAPI.sharedInstance().profileSetOnce(newProperties);
                break;
            case "profileIncrement":
                // 将用户游戏次数属性增加一次
                SensorsDataAPI.sharedInstance().profileIncrement("GamePlayed", 1);
                break;
            case "profileAppend":
                Set<String> movies = new HashSet<String>();
                movies.add("Sicario");
                movies.add("Love Letter");

                // 设定用户观影列表属性，设定后属性 "Movies" 为: ["Sicario", "Love Letter"]
                SensorsDataAPI.sharedInstance().profileAppend("Movies", movies);
                break;
            case "profileUnset":
                SensorsDataAPI.sharedInstance().profileUnset("age");
                break;
            case "track代码埋点":
                JSONObject properties = new JSONObject();
                try {
                    properties.put("ProductID", 123456);                    // 设置商品 ID
                    properties.put("ProductCatalog", "Laptop Computer");    // 设置商品类别
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                SensorsDataAPI.sharedInstance().track("BuyProduct", properties);
                break;
            case "带时长的埋点":
                // 进入商品页面
                // 调用 trackTimerStart("ViewProduct") 标记事件启动时间
                SensorsDataAPI.sharedInstance().trackTimerStart("ViewProduct");

                // ... 用户浏览商品

                // 离开商品页
                try {
                    // 在属性中记录商品 ID
                    JSONObject property = new JSONObject();
                    property.put("product_id", "apple123");
                    // 调用 track，记录 ViewProduct 事件，并在属性 event_duration 中记录用户浏览商品的时间
                    SensorsDataAPI.sharedInstance().trackTimerEnd("ViewProduct", property);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case "物品属性设置":
                // 为物品类型为 element_type 且物品 ID 为 123 的物品，设置物品属性 properties
                SensorsDataAPI.sharedInstance().itemSet("element_type", "123", null);
                break;
            case "物品属性删除":
                // 删除物品类型为 element_type 且物品 ID 为 123 的物品，设置物品属性 properties
                SensorsDataAPI.sharedInstance().itemDelete("element_type", "123");
                break;
            case "获取预置属性":
                // 获取 SDK 预置的属性
                SensorsDataAPI.sharedInstance().getPresetProperties();
                break;
            case "静态公共属性":
                // 给所有的 track 类型埋点事件设置通用属性
                JSONObject superProperty = new JSONObject();
                try {
                    superProperty.put("common","value");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                SensorsDataAPI.sharedInstance().registerSuperProperties(superProperty);
                break;
            case "flush":
                // 主动 flush 上报数据
                SensorsDataAPI.sharedInstance().flush();
                break;
            case "setMaxCacheSize":
                // 初始化时设置本地缓存大小 SDK 本地数据库默认缓存数据的上限值为 32MB
                SAConfigOptions saConfigOptions = new SAConfigOptions("SA_SERVER_URL");
                //设置本地数据缓存上限值为16MB
                saConfigOptions.setMaxCacheSize(16 * 1024 * 1024);
                break;
            case "setFlushInterval":
                // 初始化时设置数据上报的等待间隔，单位 ms
                SAConfigOptions saConfigOptions1 = new SAConfigOptions("SA_SERVER_URL");
                // 设置每 30 秒发送一次
                saConfigOptions1.setFlushInterval(30000);
                break;
            case "deleteAll":
                //删除 App 本地存储的所有事件
                SensorsDataAPI.sharedInstance().deleteAll();
                break;
            case "setSSLSocketFactory":
                // 初始化时设置自定义的签名证书
                SAConfigOptions saConfigOptions2 = new SAConfigOptions("SA_SERVER_URL");
                //构建 SSLSocketFactory 实例
                SSLSocketFactory sslSocketFactory = null;
                // 构建自定义签名证书
                saConfigOptions2.setSSLSocketFactory(sslSocketFactory);
                break;

        }
    }
}
