package com.sensorsdata.analytics.android.clockdemo;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by nomasp on 2015/10/08.
 */
public class StopWatchView extends LinearLayout{

    private int tenMSecs = 0;
    private Timer timer = new Timer();
    private TimerTask timerTask = null;
    private TimerTask showTimeTask = null;

    private TextView tvHour, tvMinute, tvSecond, tvMSecond;
    private Button btnSWStart, btnSWResume, btnSWReset, btnSWPause, btnSWRecord;
    private ListView lvWatchTimeList;
    private ArrayAdapter<String> adapter;

    private static final int MSG_WHAT_SHOW_TIME = 1;

    public StopWatchView(Context context) {
        super(context);
    }

    public StopWatchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StopWatchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate(){
        super.onFinishInflate();

        tvHour = (TextView)findViewById(R.id.tvHour);
        tvHour.setText("0");
        tvMinute = (TextView)findViewById(R.id.tvMinute);
        tvMinute.setText("0");
        tvSecond = (TextView)findViewById(R.id.tvSecond);
        tvSecond.setText("0");
        tvMSecond = (TextView)findViewById(R.id.tvMSceond);
        tvMSecond.setText("0");

        btnSWRecord = (Button)findViewById(R.id.btnSWRecord);
        btnSWRecord.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.insert(String.format("%d:%d:%d.%d",
                        tenMSecs/100/60/60,
                        tenMSecs/100/60%60,
                        tenMSecs/100%60,
                        tenMSecs%100),
                        0);
            }
        });

        btnSWPause = (Button)findViewById(R.id.btnSWPause);
        btnSWPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();

                btnSWPause.setVisibility(View.GONE);
                btnSWResume.setVisibility(View.VISIBLE);
                btnSWReset.setVisibility(View.VISIBLE);
                btnSWRecord.setVisibility(View.GONE);
            }
        });

        btnSWReset = (Button)findViewById(R.id.btnSWReset);
        btnSWReset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();
                tenMSecs = 0;
                adapter.clear();

                btnSWStart.setVisibility(View.VISIBLE);
                btnSWPause.setVisibility(View.GONE);
                btnSWReset.setVisibility(View.GONE);
                btnSWRecord.setVisibility(View.GONE);
                btnSWResume.setVisibility(View.GONE);
            }
        });

        btnSWResume = (Button)findViewById(R.id.btnSWResume);
        btnSWResume.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer();

                btnSWResume.setVisibility(View.GONE);
                btnSWReset.setVisibility(View.GONE);
                btnSWRecord.setVisibility(View.VISIBLE);
                btnSWPause.setVisibility(View.VISIBLE);
            }
        });

        btnSWStart = (Button)findViewById(R.id.btnSWStart);
        btnSWStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer();

                btnSWStart.setVisibility(View.GONE);
                btnSWPause.setVisibility(View.VISIBLE);
                btnSWRecord.setVisibility(View.VISIBLE);
            }
        });

        btnSWRecord.setVisibility(View.GONE);
        btnSWPause.setVisibility(View.GONE);
        btnSWReset.setVisibility(View.GONE);
        btnSWResume.setVisibility(View.GONE);

        lvWatchTimeList = (ListView)findViewById(R.id.lvWatchTimeList);
        adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_list_item_1);
        lvWatchTimeList.setAdapter(adapter);

        showTimeTask = new TimerTask() {
            @Override
            public void run() {
                handler.sendEmptyMessage(MSG_WHAT_SHOW_TIME);
            }
        };
        timer.schedule(showTimeTask,200,200);
    }

    private void startTimer(){
        if(timerTask == null){
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    tenMSecs++;
                }
            };
            timer.schedule(timerTask,10,10);
        }
    }

    private void stopTimer(){
        if(timerTask != null){
            timerTask.cancel();
            timerTask = null;
        }
    }

    public void onDestory(){
        timer.cancel();
    }

    private Handler handler = new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case MSG_WHAT_SHOW_TIME:
                    tvHour.setText(tenMSecs/100/60/60+"");
                    tvMinute.setText(tenMSecs/100/60%60+"");
                    tvSecond.setText(tenMSecs/100%60+"");
                    tvMSecond.setText(tenMSecs%100+"");
                    break;
                default:
                    break;
            }
        };
    };
}
