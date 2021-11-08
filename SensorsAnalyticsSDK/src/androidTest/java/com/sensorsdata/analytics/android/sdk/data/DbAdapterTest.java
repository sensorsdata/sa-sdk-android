package com.sensorsdata.analytics.android.sdk.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.sensorsdata.analytics.android.sdk.data.adapter.DbAdapter;
import com.sensorsdata.analytics.android.sdk.data.adapter.DbParams;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class DbAdapterTest {
    private static Context context;
    private static Uri mUri;
    @BeforeClass
    public static void initInstance() {
        context = ApplicationProvider.getApplicationContext();
        DbAdapter.getInstance(context, context.getPackageName(), null);
        mUri = Uri.parse("content://" + context.getPackageName() + ".SensorsDataContentProvider/events");
    }

    @Test
    public void addJSON() {
        try {
            DbAdapter.getInstance().deleteAllEvents();
            Thread.sleep(1000);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("test", "test");
            int count = DbAdapter.getInstance().addJSON(jsonObject);
            Thread.sleep(1000);
            assertEquals(count, 1);
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(mUri, null, null, null, null);
            Thread.sleep(1000);
            assertNotNull(cursor);
            assertEquals(cursor.getCount(), 1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void addJSONList() {
        try {
            DbAdapter.getInstance().deleteAllEvents();
            Thread.sleep(1000);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("test", "test");

            int count = DbAdapter.getInstance().addJSON(jsonObject);
            Thread.sleep(1000);
            assertEquals(count, 3);
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(mUri, null, null, null, null);
            Thread.sleep(1000);
            assertNotNull(cursor);
            assertEquals(cursor.getCount(), 3);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void deleteAllEvents() {
        try {
            DbAdapter.getInstance().deleteAllEvents();
            Thread.sleep(1000);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("test", "test");

            int count = DbAdapter.getInstance().addJSON(jsonObject);
            Thread.sleep(1000);
            assertEquals(count, 3);
            // 清空数据库
            DbAdapter.getInstance().deleteAllEvents();
            Thread.sleep(1000);
            ContentResolver resolver = context.getContentResolver();
            Cursor cursor = resolver.query(mUri, null, null, null, null);
            Thread.sleep(1000);
            assertNotNull(cursor);
            assertEquals(cursor.getCount(), 0);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
