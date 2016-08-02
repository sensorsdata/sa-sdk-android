package com.sensorsdata.analytics.android.clockdemo;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.logging.LogRecord;

/**
 * Created by nomasp on 2015/10/04.
 */
public class TimeView extends LinearLayout{

    private TextView tvTime;


    public TimeView(Context context) {
        super(context);
    }

    public TimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate(){
        super.onFinishInflate();

        tvTime = (TextView)findViewById(R.id.tvTime);
        tvTime.setText("H");

        timeHandler.sendEmptyMessage(0);
    }

    private Handler timeHandler = new Handler() {

        public void handleMessage(Message msg){
            refreshTime();

            if(getVisibility() == View.VISIBLE){
                timeHandler.sendEmptyMessageDelayed(0, 1000);
            }
        }
    };

    private void refreshTime(){
        Calendar c = Calendar.getInstance();

        tvTime.setText(String.format("%d:%d:%d",
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                c.get(Calendar.SECOND)));
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if(visibility == View.VISIBLE){
            timeHandler.sendEmptyMessage(0);
        }else{
            timeHandler.removeMessages(0);
        }
    }
}
