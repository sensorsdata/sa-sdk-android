package com.sensorsdata.analytics.android.sdk;

import android.net.Uri;
import android.text.TextUtils;

/**
 * Created by 王灼洲 on 2018/1/2
 */

public class ServerUrl {
    private String url;
    private String host;
    private String project;
    private String token;

    private ServerUrl() {

    }

    public ServerUrl(String url) {
        this.url = url;
        if (!TextUtils.isEmpty(url)) {
            Uri uri = Uri.parse(url);
            try {
                host = uri.getHost();
                token = uri.getQueryParameter("token");
                project = uri.getQueryParameter("project");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (TextUtils.isEmpty(host)) {
                    host = "";
                }
                if (TextUtils.isEmpty(project)) {
                    project = "default";
                }
                if (TextUtils.isEmpty(token)) {
                    token = "";
                }
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public String getHost() {
        return host;
    }

    public String getProject() {
        return project;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "url=" + url + "," +
                "host=" + host + "," +
                "project=" + project + "," +
                "token=" + token;
    }

    public boolean check(ServerUrl serverUrl) {
        try {
            if (serverUrl != null) {
//                if (!TextUtils.isEmpty(serverUrl.getToken()) && !TextUtils.isEmpty(getToken())) {
//                    if (serverUrl.getToken().equals(getToken())) {
//                        return true;
//                    }
//                } else {
                    if (getHost().equals(serverUrl.getHost()) &&
                            getProject().equals(serverUrl.getProject())) {
                        return true;
                    }
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
