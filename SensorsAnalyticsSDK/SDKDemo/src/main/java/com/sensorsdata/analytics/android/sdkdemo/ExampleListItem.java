package com.sensorsdata.analytics.android.sdkdemo;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.view.View;
import android.util.AttributeSet;
import android.util.Log;

public class ExampleListItem extends RelativeLayout implements View.OnClickListener{
    private ItemAccessibilityDelegate mAD;
    private ImageView mImage;
    private TextView mText;

    public ExampleListItem(Context context) {
        super(context);
        init();
    }
    public ExampleListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        mAD = new ItemAccessibilityDelegate();
        setAccessibilityDelegate(mAD);

        this.setOnClickListener(this);
    } 
    
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        mImage = (ImageView) findViewById(R.id.image);
        mText = (TextView) findViewById(R.id.text);
    }
    
    public final void bind(Context context, int imageId, String text) {
        mImage.setImageResource(imageId);
        mText.setText(text);
    }
    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        Log.e("ShenLei","setSelected;seleted="+selected);
    }

    @Override
    public void onClick(View v){
        //boolean selected = v.isSelected();
        //v.setSelected(!selected);
    }

    private class ItemAccessibilityDelegate extends View.AccessibilityDelegate {
            public ItemAccessibilityDelegate(View.AccessibilityDelegate realDelegate) {
                mRealDelegate = realDelegate;
            }
            public ItemAccessibilityDelegate() {
                mRealDelegate = null;;
            }
            public View.AccessibilityDelegate getRealDelegate() {
                return mRealDelegate;
            }

            public void clean() {                
            }

            @Override
            public void sendAccessibilityEvent(View host, int eventType) {
                Log.e("ShenLei","sendAccessibilityEvent;View="+host);
                Log.e("ShenLei","sendAccessibilityEvent;eventType="+eventType);
                if (null != mRealDelegate) {
                    mRealDelegate.sendAccessibilityEvent(host, eventType);
                }
            }

            private View.AccessibilityDelegate mRealDelegate;
        }
}
