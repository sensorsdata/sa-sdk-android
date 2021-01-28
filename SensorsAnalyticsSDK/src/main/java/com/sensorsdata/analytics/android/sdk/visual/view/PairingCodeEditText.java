/*
 * Created by zhangxiangwei on 2020/07/09.
 * Copyright 2015－2020 Sensors Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sensorsdata.analytics.android.sdk.visual.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.Timer;
import java.util.TimerTask;


class PairingCodeEditText extends EditText implements IPairingCodeInterface, TextWatcher {

    private static final int DEFAULT_CURSOR_DURATION = 400;
    //需要输入的位数
    private int mFigures;
    //配对码之间的间距
    private int mPairingCodeMargin;
    //底部选中的颜色
    private int mBottomSelectedColor;
    //未选中的颜色
    private int mBottomNormalColor;
    //底线的高度
    private float mBottomLineHeight;
    //选中的背景颜色
    private int mSelectedBackgroundColor;
    //光标宽度
    private int mCursorWidth;
    //光标颜色
    private int mCursorColor;
    //光标闪烁间隔
    private int mCursorDuration;
    // 控制光标闪烁
    private boolean isCursorShowing;

    private OnPairingCodeChangedListener onCodeChangedListener;
    private int mCurrentPosition = 0;
    private int mEachRectLength = 0;
    private Paint mSelectedBackgroundPaint;
    private Paint mNormalBackgroundPaint;
    private Paint mBottomSelectedPaint;
    private Paint mBottomNormalPaint;
    private Paint mCursorPaint;
    private TimerTask mCursorTimerTask;
    private Timer mCursorTimer;

    public PairingCodeEditText(Context context) {
        this(context, null);
    }

    public PairingCodeEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PairingCodeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs();
        setBackgroundColor(getColor(android.R.color.transparent));
        initPaint();
        initCursorTimer();
        setFocusableInTouchMode(true);
        setSelection(getText().length());
        requestFocus();
        super.addTextChangedListener(this);
    }

    /**
     * 初始化paint
     */
    private void initPaint() {
        mSelectedBackgroundPaint = new Paint();
        mSelectedBackgroundPaint.setColor(mSelectedBackgroundColor);
        mNormalBackgroundPaint = new Paint();
        mNormalBackgroundPaint.setColor(getColor(android.R.color.transparent));

        mBottomSelectedPaint = new Paint();
        mBottomNormalPaint = new Paint();
        mBottomSelectedPaint.setColor(mBottomSelectedColor);
        mBottomNormalPaint.setColor(mBottomNormalColor);
        mBottomSelectedPaint.setStrokeWidth(mBottomLineHeight);
        mBottomNormalPaint.setStrokeWidth(mBottomLineHeight);

        mCursorPaint = new Paint();
        mCursorPaint.setAntiAlias(true);
        mCursorPaint.setColor(mCursorColor);
        mCursorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mCursorPaint.setStrokeWidth(mCursorWidth);
    }

    private void initAttrs() {
        mFigures = 4;
        mPairingCodeMargin = dp2px(10);
        mBottomSelectedColor = Color.parseColor("#00c48e");
        mBottomNormalColor = getColor(android.R.color.darker_gray);
        mBottomLineHeight = dp2px(2);
        mSelectedBackgroundColor = getColor(android.R.color.transparent);
        mCursorWidth = dp2px(1);
        mCursorColor = Color.parseColor("#00c48e");
        mCursorDuration = DEFAULT_CURSOR_DURATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            setLayoutDirection(LAYOUT_DIRECTION_LTR);
        }
    }

    private void initCursorTimer() {
        mCursorTimerTask = new TimerTask() {
            @Override
            public void run() {
                // 通过光标间歇性显示实现闪烁效果
                isCursorShowing = !isCursorShowing;
                postInvalidate();
            }
        };
        mCursorTimer = new Timer();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // 启动定时任务，定时刷新实现光标闪烁
        mCursorTimer.scheduleAtFixedRate(mCursorTimerTask, 0, mCursorDuration);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCursorTimer.cancel();
    }

    @Override
    final public void setCursorVisible(boolean visible) {
        super.setCursorVisible(visible);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthResult = 0, heightResult = 0;
        //最终的宽度
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
            widthResult = widthSize;
        } else {
            widthResult = getScreenWidth(getContext());
        }
        //每个矩形形的宽度
        mEachRectLength = (widthResult - (mPairingCodeMargin * (mFigures - 1))) / mFigures;
        //最终的高度
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY) {
            heightResult = heightSize;
        } else {
            heightResult = mEachRectLength;
        }
        setMeasuredDimension(widthResult, heightResult);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mCurrentPosition = getText().length();
        int width = mEachRectLength - getPaddingLeft() - getPaddingRight();
        int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        for (int i = 0; i < mFigures; i++) {
            canvas.save();
            int start = width * i + i * mPairingCodeMargin;
            int end = width + start;
            //画一个矩形
            if (i == mCurrentPosition) {//选中的下一个状态
                canvas.drawRect(start, 0, end, height, mSelectedBackgroundPaint);
            } else {
                canvas.drawRect(start, 0, end, height, mNormalBackgroundPaint);
            }
            canvas.restore();
        }
        //绘制文字
        String value = getText().toString();
        for (int i = 0; i < value.length(); i++) {
            canvas.save();
            int start = width * i + i * mPairingCodeMargin;
            float x = start + width / 2;
            TextPaint paint = getPaint();
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(getCurrentTextColor());
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            float baseline = (height - fontMetrics.bottom + fontMetrics.top) / 2
                    - fontMetrics.top;
            canvas.drawText(String.valueOf(value.charAt(i)), x, baseline, paint);
            canvas.restore();
        }
        //绘制底线
        for (int i = 0; i < mFigures; i++) {
            canvas.save();
            float lineY = height - mBottomLineHeight / 2;
            int start = width * i + i * mPairingCodeMargin;
            int end = width + start;
            if (i < mCurrentPosition) {
                canvas.drawLine(start, lineY, end, lineY, mBottomSelectedPaint);
            } else {
                canvas.drawLine(start, lineY, end, lineY, mBottomNormalPaint);
            }
            canvas.restore();
        }
        //绘制光标
        boolean isCursorVisible = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            isCursorVisible = isCursorVisible();
        }
        if (!isCursorShowing && isCursorVisible && mCurrentPosition < mFigures && hasFocus()) {
            canvas.save();
            int startX = mCurrentPosition * (width + mPairingCodeMargin) + width / 2;
            int startY = height / 4;
            int endX = startX;
            int endY = height - height / 4;
            canvas.drawLine(startX, startY, endX, endY, mCursorPaint);
            canvas.restore();
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        mCurrentPosition = getText().length();
        postInvalidate();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mCurrentPosition = getText().length();
        postInvalidate();
        if (onCodeChangedListener != null) {
            onCodeChangedListener.onPairingCodeChanged(getText(), start, before, count);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        mCurrentPosition = getText().length();
        postInvalidate();

        if (getText().length() == mFigures) {
            if (onCodeChangedListener != null) {
                onCodeChangedListener.onInputCompleted(getText());
            }
        } else if (getText().length() > mFigures) {
            getText().delete(mFigures, getText().length());
        }
    }

    @Override
    public void setFigures(int figures) {
        mFigures = figures;
        postInvalidate();
    }

    @Override
    public void setPairingCodeMargin(int margin) {
        mPairingCodeMargin = margin;
        postInvalidate();
    }

    @Override
    public void setBottomSelectedColor(int bottomSelectedColor) {
        mBottomSelectedColor = getColor(bottomSelectedColor);
        postInvalidate();
    }

    @Override
    public void setBottomNormalColor(int bottomNormalColor) {
        mBottomSelectedColor = getColor(bottomNormalColor);
        postInvalidate();
    }

    @Override
    public void setSelectedBackgroundColor(int selectedBackground) {
        mSelectedBackgroundColor = getColor(selectedBackground);
        postInvalidate();
    }

    @Override
    public void setBottomLineHeight(int bottomLineHeight) {
        this.mBottomLineHeight = bottomLineHeight;
        postInvalidate();
    }

    @Override
    public void setOnPairingCodeChangedListener(OnPairingCodeChangedListener listener) {
        this.onCodeChangedListener = listener;
    }

    private int getColor(int color) {
        return getContext().getResources().getColor(color);
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    static int getScreenWidth(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            wm.getDefaultDisplay().getMetrics(metrics);
        }
        return metrics.widthPixels;
    }

    void showKeyBoard(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(this, InputMethodManager.SHOW_FORCED);
        }
    }

    void hiddenKeyBord() {
        InputMethodManager mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mInputMethodManager != null) {
            mInputMethodManager.hideSoftInputFromWindow(this.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
    }

    void clearText() {
        getText().delete(0, getText().length());
    }
}
