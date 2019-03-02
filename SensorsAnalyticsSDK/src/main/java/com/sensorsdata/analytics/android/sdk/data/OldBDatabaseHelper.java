/**Created by wangzhuozhou on 2015/08/01.
 * Copyright © 2015－2018 Sensors Data Inc. All rights reserved. */
 
package com.sensorsdata.analytics.android.sdk.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by 王灼洲 on 2017/5/5
 */
public class OldBDatabaseHelper extends SQLiteOpenHelper {
    public OldBDatabaseHelper(Context context, String dbName) {
        super(context, dbName, null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public JSONArray getAllEvents() {
        final JSONArray arr = new JSONArray();
        Cursor c = null;
        try {
            final SQLiteDatabase db = getReadableDatabase();
            c = db.rawQuery("SELECT * FROM " + DbParams.TABLE_EVENTS +
                    " ORDER BY " + DbParams.KEY_CREATED_AT, null);
            while (c.moveToNext()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("created_at", c.getString(c.getColumnIndex("created_at")));
                jsonObject.put("data", c.getString(c.getColumnIndex("data")));
                arr.put(jsonObject);
            }
        } catch (Exception e) {
            com.sensorsdata.analytics.android.sdk.SALog.printStackTrace(e);
        } finally {
            close();
            if (c != null) {
                c.close();
            }
        }

        return arr;
    }
}
