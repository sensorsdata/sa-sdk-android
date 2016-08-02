package com.sensorsdata.analytics.android.clockdemo;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TimePicker;

import java.util.Calendar;

/**
 * Created by nomasp on 2015/10/07.
 */
public class AlarmView extends LinearLayout {


  private Button btnAddAlarm;
  private ListView lvAlarmList;
  private ArrayAdapter<AlarmData> adapter;
  private static final String KEY_ALARM = "alarmlist";
  private AlarmManager alarmManager;

  public AlarmView(Context context) {
    super(context);
    init();
  }

  public AlarmView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public AlarmView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    if (isInEditMode()) {
      return;
    }
    alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    btnAddAlarm = (Button) findViewById(R.id.btnAddAlarm);
    lvAlarmList = (ListView) findViewById(R.id.lvAlarmList);

    adapter = new ArrayAdapter<AlarmData>(getContext(),
        android.R.layout.simple_list_item_1);

    //adapter.add(new AlarmData(System.currentTimeMillis()));
    lvAlarmList.setAdapter(adapter);

    readSavedAlarmList();

    btnAddAlarm.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        addAlarm();
      }
    });

    lvAlarmList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view, final int position,
          long id) {

        new AlertDialog.Builder(getContext()).setTitle("操作选项").setItems(
            new CharSequence[] {"删除", "全部删除"}, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                  case 0:
                    deleteAlarm(position);
                    break;
                  case 1:
                    deleteAllAlarm();
                    break;
                  default:
                    break;
                }
              }
            }
        ).setNegativeButton("取消", null).show();

        return true;
      }
    });

  }

  private void addAlarm() {

    Calendar c = Calendar.getInstance();

    new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
      @Override
      public void onTimeSet(TimePicker view, int hourofDay, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourofDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Calendar currentTime = Calendar.getInstance();

        if (calendar.getTimeInMillis() <= currentTime.getTimeInMillis()) {
          calendar.setTimeInMillis(calendar.getTimeInMillis() + 24 * 60 * 60 * 1000);
        }

        AlarmData ad = new AlarmData(calendar.getTimeInMillis());
        adapter.add(ad);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            5 * 60 * 1000,
            PendingIntent.getBroadcast(getContext(),
                ad.getId(),
                new Intent(getContext(),
                    AlarmReceiver.class),
                0));
        saveAlarmList();
      }
    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
  }

  private void saveAlarmList() {
    Editor editor = getContext().getSharedPreferences(
        AlarmView.class.getName(),
        Context.MODE_PRIVATE).edit();

    StringBuffer sb = new StringBuffer();

    for (int i = 0; i < adapter.getCount(); i++) {
      sb.append(adapter.getItem(i).getTime()).append(",");
    }

    if (sb.length() > 1) {
      String content = sb.toString().substring(0, sb.length() - 1);

      editor.putString(KEY_ALARM, content);

      System.out.println(content);
    } else {
      editor.putString(KEY_ALARM, null);
    }

    editor.commit();

  }

  private void readSavedAlarmList() {
    SharedPreferences sp = getContext().getSharedPreferences(
        AlarmView.class.getName(), Context.MODE_PRIVATE);
    String content = sp.getString(KEY_ALARM, null);

    if (content != null) {
      String[] timeStrings = content.split(",");
      for (String str : timeStrings) {
        adapter.add(new AlarmData(Long.parseLong(str)));
      }
    }
  }

  private void deleteAlarm(int position) {
    AlarmData ad = adapter.getItem(position);
    adapter.remove(ad);

    saveAlarmList();

    alarmManager.cancel(PendingIntent.getBroadcast(getContext(), ad.getId(),
        new Intent(getContext(), AlarmReceiver.class), 0));
  }

  private void deleteAllAlarm() {

    int adapterCount = adapter.getCount();   // 为adapter的个数进行计数
    AlarmData ad;
    for (int i = 0; i < adapterCount; i++) {
      ad = adapter.getItem(0);       // 每次从第1个开始移除
      adapter.remove(ad);

      saveAlarmList();       // 移除后重新保存列表

      alarmManager.cancel(PendingIntent.getBroadcast(getContext(), ad.getId(),
          new Intent(getContext(), AlarmReceiver.class), 0));   // 取消闹钟的广播
    }
  }

  private static class AlarmData {

    private String timeLabel = "";
    private long time = 0;
    private Calendar date;

    public AlarmData(long time) {
      this.time = time;
      date = Calendar.getInstance();
      date.setTimeInMillis(time);
      timeLabel = String.format("%d月%d日 %d:%d",
          date.get(Calendar.MONTH) + 1,
          date.get(Calendar.DAY_OF_MONTH),
          date.get(Calendar.HOUR_OF_DAY),
          date.get(Calendar.MINUTE));
    }

    public long getTime() {
      return time;
    }

    public String getTimeLabel() {
      return timeLabel;
    }

    public int getId() {
      return (int) (getTime() / 1000 / 60);
    }

    @Override
    public String toString() {
      return getTimeLabel();
    }
  }
}
