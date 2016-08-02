package com.sensorsdata.analytics.android.clockdemo;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.LogRecord;

/**
 * Created by nomasp on 2015/10/07.
 */
public class TimerView extends LinearLayout {

    private Button btnStart, btnPause, btnResume, btnReset;
    private EditText etHour, etMinute, etSecond;

    private static final int MSG_WHAT_TIME_IS_UP = 1;
    private static final int MSG_WHAT_TIME_TICK = 2;

    private int allTimerCount = 0;
    private Timer timer = new Timer();
    private TimerTask timerTask = null;

    public TimerView(Context context) {
        super(context);
    }

    public TimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate(){
        super.onFinishInflate();

        btnPause = (Button)findViewById(R.id.btnPause);
        btnPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();

                btnPause.setVisibility(View.GONE);
                btnResume.setVisibility(View.VISIBLE);

            }
        });

        btnReset = (Button)findViewById(R.id.btnReset);
        btnReset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();

                etHour.setText("0");
                etMinute.setText("0");
                etSecond.setText("0");

                btnReset.setVisibility(View.GONE);
                btnResume.setVisibility(View.GONE);
                btnPause.setVisibility(View.GONE);
                btnStart.setVisibility(View.VISIBLE);
            }
        });

        btnResume = (Button)findViewById(R.id.btnResume);
        btnResume.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startTime();

                btnResume.setVisibility(View.GONE);
                btnPause.setVisibility(View.VISIBLE);
            }
        });

        btnStart = (Button)findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startTime();

                btnStart.setVisibility(View.GONE);
                btnPause.setVisibility(View.VISIBLE);
                btnReset.setVisibility(View.VISIBLE);
            }
        });

        etHour = (EditText)findViewById(R.id.etHour);
        etMinute = (EditText)findViewById(R.id.etMinute);
        etSecond = (EditText)findViewById(R.id.etSecond);

        etHour.setText("00");
        etHour.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s)) {
                    int value = Integer.parseInt(s.toString());

                    if (value > 59) {
                        etHour.setText("59");
                    } else if (value < 0) {
                        etHour.setText("0");
                    }
                }
                checkToEnableBtnStart();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etMinute.setText("00");
        etMinute.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s)) {
                    int value = Integer.parseInt(s.toString());

                    if (value > 59) {
                        etMinute.setText("59");
                    } else if (value < 0) {
                        etMinute.setText("0");
                    }
                }
                checkToEnableBtnStart();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        etSecond.setText("00");
        etSecond.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!TextUtils.isEmpty(s)){
                    int value = Integer.parseInt(s.toString());

                    if(value > 59){
                        etSecond.setText("59");
                    }else if(value < 0){
                        etSecond.setText("0");
                    }
                }
                checkToEnableBtnStart();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        btnStart.setVisibility(View.VISIBLE);
        btnPause.setVisibility(View.GONE);
        btnResume.setVisibility(View.GONE);
        btnReset.setVisibility(View.GONE);
    }

    private void checkToEnableBtnStart(){
        btnStart.setEnabled((!TextUtils.isEmpty(etHour.getText())
                &&Integer.parseInt(etHour.getText().toString()) > 0)||
                (!TextUtils.isEmpty(etMinute.getText())&&
                Integer.parseInt(etMinute.getText().toString()) > 0)||
                (!TextUtils.isEmpty(etSecond.getText())&&
                Integer.parseInt(etSecond.getText().toString()) > 0));
    }

    private void startTime(){
        if(timerTask == null){
            allTimerCount = Integer.parseInt(etHour.getText().toString())*60*60
                    + Integer.parseInt(etMinute.getText().toString())*60
                    + Integer.parseInt(etSecond.getText().toString());
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    allTimerCount-- ;

                    handler.sendEmptyMessage(MSG_WHAT_TIME_TICK);

                    if(allTimerCount <= 0){
                        handler.sendEmptyMessage(MSG_WHAT_TIME_IS_UP);
                        stopTimer();
                    }
                }
            };

            timer.schedule(timerTask,1000,1000);
        }
    }

    private void stopTimer(){
        if(timerTask != null){
            timerTask.cancel();
            timerTask = null;
        }
    }

    private Handler handler = new Handler() {
        public void handleMessage(Message msg){
            switch (msg.what){
                case MSG_WHAT_TIME_TICK:

                    int hour = allTimerCount/60/60;
                    int min = (allTimerCount/60)%60;
                    int sec = allTimerCount%60;

                    etHour.setText(hour + "");
                    etMinute.setText(min + "");
                    etSecond.setText(sec + "");

                    break;
                case MSG_WHAT_TIME_IS_UP:

                    new AlertDialog.Builder(getContext()).setTitle("Time is up")
                            .setMessage("Message: Time is up")
                            .setNegativeButton("Cancel",null)
                            .show();

                    btnReset.setVisibility(View.GONE);
                    btnResume.setVisibility(View.GONE);
                    btnPause.setVisibility(View.GONE);
                    btnStart.setVisibility(View.VISIBLE);

                    break;
                default:
                    break;
            }
        }
    };
}


// 85