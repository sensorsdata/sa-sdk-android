package com.sensorsdata.analytics.android.sdk;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/* package */ class DbAdapter {

    private static final String TAG = "SA.DbAdapter";
    private final File mDatabaseFile;
    private Uri mUri;

    public enum Table {
        EVENTS("events");

        Table(String name) {
            mTableName = name;
        }

        public String getName() {
            return mTableName;
        }

        private final String mTableName;
    }

    public static final String KEY_DATA = "data";
    public static final String KEY_CREATED_AT = "created_at";

    public static final int DB_UPDATE_ERROR = -1;
    public static final int DB_OUT_OF_MEMORY_ERROR = -2;

    private final Context mContext;

    private long getMaxCacheSize(Context context) {
        try {
            return SensorsDataAPI.sharedInstance(context).getMaxCacheSize();
        } catch (Exception e) {
            e.printStackTrace();
            return 32 * 1024 * 1024;
        }
    }

    private boolean belowMemThreshold() {
        if (mDatabaseFile.exists()) {
            return Math.max(
                    mDatabaseFile.getUsableSpace(),
                    getMaxCacheSize(mContext)
            ) >= mDatabaseFile.length();
        }
        return true;
    }

    private ContentResolver contentResolver;

    public DbAdapter(Context context, String dbName) {
        mContext = context;
        contentResolver = mContext.getContentResolver();
        mDatabaseFile = context.getDatabasePath(dbName);
        mUri = Uri.parse("content://" + dbName + ".SensorsDataContentProvider/" + Table.EVENTS.getName());
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param j     the JSON to record
     * @param table the table to insert into, either "events" or "people"
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addJSON(JSONObject j, Table table) {
        // we are aware of the race condition here, but what can we do..?
        int count = DB_UPDATE_ERROR;
        Cursor c = null;
        try {
            if (!belowMemThreshold()) {
                SALog.i(TAG, "There is not enough space left on the device to store events, so will delete some old events");
                String[] eventsData = generateDataString(DbAdapter.Table.EVENTS, 100);
                if (eventsData == null) {
                    return DB_OUT_OF_MEMORY_ERROR;
                }
                final String lastId = eventsData[0];
                count = cleanupEvents(lastId, DbAdapter.Table.EVENTS);
                if (count <= 0) {
                    return DB_OUT_OF_MEMORY_ERROR;
                }
            }

            final ContentValues cv = new ContentValues();
            cv.put(KEY_DATA, j.toString() + "\t" + j.toString().hashCode());
            cv.put(KEY_CREATED_AT, System.currentTimeMillis());
            contentResolver.insert(mUri, cv);
            c = contentResolver.query(mUri, null, null, null, null);
            if (c != null) {
                count = c.getCount();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } finally {

            }
        }
        return count;
    }

    /**
     * Removes events with an _id <= last_id from table
     *
     * @param last_id the last id to delete
     * @param table   the table to remove events from
     * @return the number of rows in the table
     */
    public int cleanupEvents(String last_id, Table table) {
        Cursor c = null;
        int count = DB_UPDATE_ERROR;

        try {
            contentResolver.delete(mUri, "_id <= ?", new String[]{last_id});
            c = contentResolver.query(mUri, null, null, null, null);
            if (c != null) {
                count = c.getCount();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } finally {

            }
        }
        return count;
    }

    public String[] generateDataString(Table table, int limit) {
        Cursor c = null;
        String data = null;
        String last_id = null;
        final String tableName = table.getName();
        try {
            c = contentResolver.query(mUri, null, null, null, KEY_CREATED_AT + " ASC LIMIT " + String.valueOf(limit));
            final JSONArray arr = new JSONArray();

            if (c != null) {
                while (c.moveToNext()) {
                    if (c.isLast()) {
                        last_id = c.getString(c.getColumnIndex("_id"));
                    }
                    try {
                        String keyData = c.getString(c.getColumnIndex(KEY_DATA));
                        if (!TextUtils.isEmpty(keyData)) {
                            JSONObject j = null;
                            int index = keyData.lastIndexOf("\t");
                            if (index > -1) {
                                String crc = keyData.substring(index).replaceFirst("\t", "");
                                String content = keyData.substring(0, index);
                                if (!TextUtils.isEmpty(content) && !TextUtils.isEmpty(crc)) {
                                    if (crc.equals(String.valueOf(content.hashCode()))) {
                                        j = new JSONObject(content);
                                    }
                                }
                            } else {
                                j = new JSONObject(keyData);
                            }
                            if (j != null) {
                                j.put("_flush_time", System.currentTimeMillis());
                                arr.put(j);
                            }
                        }
                    } catch (final JSONException e) {
                        // Ignore this object
                    }
                }

                if (arr.length() > 0) {
                    data = arr.toString();
                }
            }
        } catch (final SQLiteException e) {
            SALog.i(TAG, "Could not pull records for SensorsData out of database " + tableName
                    + ". Waiting to send.", e);
            last_id = null;
            data = null;
        } finally {
            if (c != null) {
                c.close();
            }
        }

        if (last_id != null && data != null) {
            final String[] ret = {last_id, data};
            return ret;
        }
        return null;
    }

}
