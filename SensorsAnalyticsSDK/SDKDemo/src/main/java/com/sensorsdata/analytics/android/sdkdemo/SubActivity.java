package com.sensorsdata.analytics.android.sdkdemo;

import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class SubActivity extends Activity {

  private static final String LOGTAG = "SensorsData Example Application";

  private SensorsDataAPI mSensorsData;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sub);
    ShowDialog();
  }

  private void ShowDialog() {
    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int whichButton) {
        dialog.dismiss();
        jumpToExampleListActivity();
      }
    };

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setCancelable(true);
    builder.setMessage("123456");
    builder.setPositiveButton("OK", listener);
    builder.setNegativeButton("Cancel", null);
    AlertDialog ad = builder.create();
    ad.show();
  }

  public void jumpToExampleListActivity() {
    Intent intent = new Intent(this, ExampleListActivity.class);
    startActivity(intent);
  }

}
