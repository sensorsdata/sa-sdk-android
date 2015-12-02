package com.sensorsdata.analytics.android.sdkdemo;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.SensorsDataException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class MainActivity extends Activity {

  private static final String LOGTAG = "SensorsData Example Application";

  private SensorsDataAPI mSensorsData;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String userId = "SensorsDataAndroidSDKTest";

    try {
      mSensorsData = SensorsDataAPI.getInstance(this);
      mSensorsData.identify(userId);
    } catch (SensorsDataException e) {
      e.printStackTrace();
    }

    setContentView(R.layout.activity_main);
  }

  @Override protected void onResume() {
    super.onResume();

    try {
      final JSONObject properties = new JSONObject();
      properties.put("AppVersion", "1.1");
      mSensorsData.track("AppResumed", properties);
    } catch (final JSONException e) {
      e.printStackTrace();
    } catch (SensorsDataException e) {
      e.printStackTrace();
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    mSensorsData.flush();
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public void sendToSensorsData(final View view) {
    final EditText firstNameEdit = (EditText) findViewById(R.id.edit_first_name);
    final EditText lastNameEdit = (EditText) findViewById(R.id.edit_last_name);
    final EditText emailEdit = (EditText) findViewById(R.id.edit_email_address);

    final String firstName = firstNameEdit.getText().toString();
    final String lastName = lastNameEdit.getText().toString();
    final String email = emailEdit.getText().toString();

    try {
      JSONObject properties = new JSONObject();
      properties.put("first_name", firstName);
      properties.put("last_name", lastName);
      properties.put("email", email);
      mSensorsData.profileSet(properties);
      mSensorsData.profileIncrement("count", 1);
      mSensorsData.track("ButtonClicked");
    } catch (SensorsDataException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }

  }

}
