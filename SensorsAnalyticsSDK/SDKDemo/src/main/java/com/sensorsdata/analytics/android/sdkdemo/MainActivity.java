package com.sensorsdata.analytics.android.sdkdemo;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;
import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

  private static final String LOGTAG = "SensorsData Example Application";

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String userId = "SensorsDataAndroidSDKTest";

    try {
      SensorsDataAPI sa = SensorsDataAPI.sharedInstance(
          this,
          "http://sa_host:8006/sa",
          SensorsDataAPI.DebugMode.DEBUG_OFF);
      sa.identify(userId);
    } catch (InvalidDataException e) {
      e.printStackTrace();
    }

    setContentView(R.layout.activity_main);
  }

  @Override protected void onResume() {
    super.onResume();

    try {
      SensorsDataAPI.sharedInstance(this).track("AppResumed", null);
    } catch (InvalidDataException e) {
      e.printStackTrace();
    }
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    //    try {
    //      SensorsDataAPI.sharedInstance(this).flush();
    //    } catch (SensorsDataException e) {
    //      e.printStackTrace();
    //    }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    return ((id == R.id.action_settings) || super.onOptionsItemSelected(item));
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
      SensorsDataAPI.sharedInstance(this).track("ButtonClicked");
    } catch (InvalidDataException e) {
      e.printStackTrace();
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public void jumpToActivity(final View view) {
    Intent intent = new Intent(this, ExampleListActivity.class);
    startActivity(intent);
  }
}
