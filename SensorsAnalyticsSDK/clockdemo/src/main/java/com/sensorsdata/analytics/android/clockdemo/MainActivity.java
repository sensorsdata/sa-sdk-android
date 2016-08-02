package com.sensorsdata.analytics.android.clockdemo;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TabHost;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

  private static TabHost tabHost;

  // Sensors Analytics 采集数据的地址
  private final static String SA_SERVER_URL = "http://test-ckh-zyh.cloud.sensorsdata.cn:8006/sa?token=de28ecf691865360";
  // Sensors Analytics 配置分发的地址
  private final static String SA_CONFIGURE_URL = "http://test-ckh-zyh.cloud.sensorsdata.cn/api/vtrack/config";
  // Sensors Analytics DEBUG 模式
  //   SensorsDataAPI.DebugMode.DEBUG_OFF - 关闭 Debug 模式
  //   SensorsDataAPI.DebugMode.DEBUG_ONLY - 打开 Debug 模式，校验数据，但不进行数据导入
  //   SensorsDataAPI.DebugMode.DEBUG_AND_TRACK - 打开 Debug 模式，校验数据，并将数据导入到 Sensors Analytics 中
  // 注意！请不要在正式发布的 App 中使用 Debug 模式！
  private final SensorsDataAPI.DebugMode SA_DEBUG_MODE = SensorsDataAPI.DebugMode.DEBUG_AND_TRACK;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    tabHost = (TabHost) findViewById(R.id.tabHost);
    tabHost.setup();

    tabHost.addTab(tabHost.newTabSpec("tabTimer").
        setIndicator("时钟").setContent(R.id.tabTime));
    tabHost.addTab(tabHost.newTabSpec("tabAlarm").
        setIndicator("闹钟").setContent(R.id.tabAlarm));
    tabHost.addTab(tabHost.newTabSpec("tabTimer").
        setIndicator("计时器").setContent(R.id.tabTimer));
    tabHost.addTab(tabHost.newTabSpec("tabStopWatch").
        setIndicator("秒表").setContent(R.id.tabStopWatch));

    // 初始化 Sensors Analytics SDK
    SensorsDataAPI.sharedInstance(
        this,                               // 传入 Context
        SA_SERVER_URL,                      // 数据接收的 URL
        SA_CONFIGURE_URL,                   // 配置分发的 URL
        SA_DEBUG_MODE);                     // Debug 模式选项
    SensorsDataAPI.sharedInstance(this).enableAutoTrack();
    SensorsDataAPI.sharedInstance(this).enableEditingVTrack();

    // 追踪 "应用启动" 事件
    try {
      JSONObject properties = new JSONObject();
      properties.put("hahah", "abc");
      SensorsDataAPI.sharedInstance(this).registerSuperProperties(properties);
      SensorsDataAPI.sharedInstance(this).track("AppLoaded");
    } catch (InvalidDataException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

}
