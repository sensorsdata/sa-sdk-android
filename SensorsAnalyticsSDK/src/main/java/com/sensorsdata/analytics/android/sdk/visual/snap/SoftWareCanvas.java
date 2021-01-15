/*
 * Created by zhangxiangwei on 2019/12/27.
 * Copyright 2015－2021 Sensors Data Inc.
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

package com.sensorsdata.analytics.android.sdk.visual.snap;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build.VERSION;

import com.sensorsdata.analytics.android.sdk.SALog;
import com.sensorsdata.analytics.android.sdk.util.WeakSet;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Iterator;

public class SoftWareCanvas extends Canvas {
    private static final String TAG = "SA.SoftWareCanvas";
    private WeakSet<Bitmap> bitmapWeakSet = new WeakSet();
    private Bitmap mBitmap;

    public SoftWareCanvas(Bitmap bitmap) {
        super(bitmap);
        this.mBitmap = bitmap;
    }

    private Bitmap drawOnSFCanvas(Bitmap bitmap) {
        if (VERSION.SDK_INT < 26 || bitmap.getConfig() != Config.HARDWARE) {
            return bitmap;
        }
        Bitmap sfBitmap = bitmap.copy(Config.ARGB_8888, false);
        this.bitmapWeakSet.add(sfBitmap);
        return sfBitmap;
    }

    private Paint replaceBitmapShader(Paint paint) {
        if (paint == null) {
            return null;
        }
        if (VERSION.SDK_INT >= 26 && (paint.getShader() instanceof BitmapShader)) {
            Paint saPaint = new Paint(paint);
            BitmapShader userBitmapShader = (BitmapShader) saPaint.getShader();
            try {
                Field mBitmap = BitmapShader.class.getField("mBitmap");
                mBitmap.setAccessible(true);
                if (((Bitmap) mBitmap.get(userBitmapShader)).getConfig() == Config.HARDWARE) {
                    Field mTileX = BitmapShader.class.getDeclaredField("mTileX");
                    Field mTileY = BitmapShader.class.getDeclaredField("mTileY");
                    mTileX.setAccessible(true);
                    mTileY.setAccessible(true);
                    Bitmap sfBitmap = ((Bitmap) mBitmap.get(userBitmapShader)).copy(Config.ARGB_8888, false);
                    this.bitmapWeakSet.add(sfBitmap);
                    Constructor<BitmapShader> constructor = BitmapShader.class.getDeclaredConstructor(new Class[]{Bitmap.class, Integer.TYPE, Integer.TYPE});
                    constructor.setAccessible(true);
                    BitmapShader bitmapShaderShader = (BitmapShader) constructor.newInstance(new Object[]{sfBitmap, mTileX.get(userBitmapShader), mTileY.get(userBitmapShader)});
                    Matrix matrix = new Matrix();
                    paint.getShader().getLocalMatrix(matrix);
                    bitmapShaderShader.setLocalMatrix(matrix);
                    saPaint.setShader(bitmapShaderShader);
                    return saPaint;
                }
            } catch (Exception e) {
                SALog.i(TAG, e.toString());
            }
        }
        return paint;
    }

    public void destroy() {
        Iterator it = this.bitmapWeakSet.iterator();
        while (it.hasNext()) {
            ((Bitmap) it.next()).recycle();
        }
        this.bitmapWeakSet.clear();
    }

    public void drawLines(float[] pts, int offset, int count, Paint paint) {
        super.drawLines(pts, offset, count, replaceBitmapShader(paint));
    }

    public void drawBitmap(Bitmap bitmap, float left, float top, Paint paint) {
        Bitmap drawBitmap = drawOnSFCanvas(bitmap);
        if (drawBitmap.getDensity() != this.mBitmap.getDensity()) {
            int leftInt = (int) left;
            int topInt = (int) top;
            Rect rect = new Rect(leftInt, topInt, drawBitmap.getWidth() + leftInt, drawBitmap.getHeight() + topInt);
            super.drawBitmap(drawBitmap, rect, rect, replaceBitmapShader(paint));
            return;
        }
        super.drawBitmap(drawBitmap, left, top, replaceBitmapShader(paint));
    }

    public void drawBitmap(Bitmap bitmap, Rect src, RectF dst, Paint paint) {
        super.drawBitmap(drawOnSFCanvas(bitmap), src, dst, replaceBitmapShader(paint));
    }

    public void drawBitmap(Bitmap bitmap, Rect src, Rect dst, Paint paint) {
        super.drawBitmap(drawOnSFCanvas(bitmap), src, dst, replaceBitmapShader(paint));
    }

    public void drawBitmap(int[] colors, int offset, int stride, float x, float y, int width, int height, boolean hasAlpha, Paint paint) {
        super.drawBitmap(colors, offset, stride, x, y, width, height, hasAlpha, replaceBitmapShader(paint));
    }

    public void drawBitmap(int[] colors, int offset, int stride, int x, int y, int width, int height, boolean hasAlpha, Paint paint) {
        super.drawBitmap(colors, offset, stride, x, y, width, height, hasAlpha, replaceBitmapShader(paint));
    }

    public void drawBitmap(Bitmap bitmap, Matrix matrix, Paint paint) {
        super.drawBitmap(drawOnSFCanvas(bitmap), matrix, replaceBitmapShader(paint));
    }

    public void drawBitmapMesh(Bitmap bitmap, int meshWidth, int meshHeight, float[] verts, int vertOffset, int[] colors, int colorOffset, Paint paint) {
        super.drawBitmapMesh(drawOnSFCanvas(bitmap), meshWidth, meshHeight, verts, vertOffset, colors, colorOffset, replaceBitmapShader(paint));
    }

    public void drawRoundRect(RectF rect, float rx, float ry, Paint paint) {
        super.drawRoundRect(rect, rx, ry, replaceBitmapShader(paint));
    }

    public void drawRoundRect(float left, float top, float right, float bottom, float rx, float ry, Paint paint) {
        super.drawRoundRect(left, top, right, bottom, rx, ry, replaceBitmapShader(paint));
    }

    public void setBitmap(Bitmap bitmap) {
        super.setBitmap(drawOnSFCanvas(bitmap));
    }

    public int saveLayer(RectF bounds, Paint paint, int saveFlags) {
        return super.saveLayer(bounds, replaceBitmapShader(paint), saveFlags);
    }

    public int saveLayer(RectF bounds, Paint paint) {
        return super.saveLayer(bounds, replaceBitmapShader(paint));
    }

    public int saveLayer(float left, float top, float right, float bottom, Paint paint, int saveFlags) {
        return super.saveLayer(left, top, right, bottom, replaceBitmapShader(paint), saveFlags);
    }

    public int saveLayer(float left, float top, float right, float bottom, Paint paint) {
        return super.saveLayer(left, top, right, bottom, replaceBitmapShader(paint));
    }

    public void drawArc(RectF oval, float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
        super.drawArc(oval, startAngle, sweepAngle, useCenter, replaceBitmapShader(paint));
    }

    public void drawCircle(float cx, float cy, float radius, Paint paint) {
        super.drawCircle(cx, cy, radius, replaceBitmapShader(paint));
    }

    public void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
        super.drawLine(startX, startY, stopX, stopY, replaceBitmapShader(paint));
    }

    public void drawLines(float[] pts, Paint paint) {
        super.drawLines(pts, replaceBitmapShader(paint));
    }

    public void drawOval(RectF oval, Paint paint) {
        super.drawOval(oval, replaceBitmapShader(paint));
    }

    public void drawOval(float left, float top, float right, float bottom, Paint paint) {
        super.drawOval(left, top, right, bottom, replaceBitmapShader(paint));
    }

    public void drawPaint(Paint paint) {
        super.drawPaint(replaceBitmapShader(paint));
    }

    public void drawPath(Path path, Paint paint) {
        super.drawPath(path, replaceBitmapShader(paint));
    }

    public void drawPoint(float x, float y, Paint paint) {
        super.drawPoint(x, y, replaceBitmapShader(paint));
    }

    public void drawPoints(float[] pts, int offset, int count, Paint paint) {
        super.drawPoints(pts, offset, count, replaceBitmapShader(paint));
    }

    public void drawPoints(float[] pts, Paint paint) {
        super.drawPoints(pts, replaceBitmapShader(paint));
    }

    public void drawPosText(char[] text, int index, int count, float[] pos, Paint paint) {
        super.drawPosText(text, index, count, pos, replaceBitmapShader(paint));
    }

    public void drawPosText(String text, float[] pos, Paint paint) {
        super.drawPosText(text, pos, replaceBitmapShader(paint));
    }

    public void drawRect(RectF rect, Paint paint) {
        super.drawRect(rect, replaceBitmapShader(paint));
    }

    public void drawRect(Rect r, Paint paint) {
        super.drawRect(r, replaceBitmapShader(paint));
    }

    public void drawRect(float left, float top, float right, float bottom, Paint paint) {
        super.drawRect(left, top, right, bottom, replaceBitmapShader(paint));
    }

    public void drawText(char[] text, int index, int count, float x, float y, Paint paint) {
        super.drawText(text, index, count, x, y, replaceBitmapShader(paint));
    }

    public void drawText(String text, float x, float y, Paint paint) {
        super.drawText(text, x, y, replaceBitmapShader(paint));
    }

    public void drawText(String text, int start, int end, float x, float y, Paint paint) {
        super.drawText(text, start, end, x, y, replaceBitmapShader(paint));
    }

    public void drawText(CharSequence text, int start, int end, float x, float y, Paint paint) {
        super.drawText(text, start, end, x, y, replaceBitmapShader(paint));
    }

    public void drawTextOnPath(char[] text, int index, int count, Path path, float hOffset, float vOffset, Paint paint) {
        super.drawTextOnPath(text, index, count, path, hOffset, vOffset, replaceBitmapShader(paint));
    }

    public void drawTextOnPath(String text, Path path, float hOffset, float vOffset, Paint paint) {
        super.drawTextOnPath(text, path, hOffset, vOffset, replaceBitmapShader(paint));
    }
}