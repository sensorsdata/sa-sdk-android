/*
 * Created by chenru on 2022/7/4 下午3:50.
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

package com.sensorsdata.analytics.android.sdk.advert.model;

public class SASlinkResponse {
    /**
     * 创建结果 Code
     * 0：创建成功
     * 10001：必要参数缺失
     * 10002：无网络
     * 10003：广告自定义域名未设或填写错误
     * 10004：后端返回数据异常
     * 10005：创建短链时未设置回调对象
     * 4XX、5XX：网络异常
     */
    public int statusCode;
    /**
     * 创建结果信息
     */
    public String message;
    /**
     * 生成的 slink 分享链接，创建失败则为空字符串
     */
    public String slink;
    /**
     * 通用跳转链接
     */
    public String commonRedirectURI;
    public String slinkID;

    @Override
    public String toString() {
        return "SASlinkResponse{" +
                "statusCode=" + statusCode +
                ", message='" + message + '\'' +
                ", slink='" + slink + '\'' +
                ", slinkID='" + slinkID + '\'' +
                ", commonRedirectURI='" + commonRedirectURI + '\'' +
                '}';
    }
}
