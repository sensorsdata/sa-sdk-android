package com.sensorsdata.analytics.android.sdk;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

// In order to use writeEdits, we have to suppress the linter's check for commit()/apply()
@SuppressLint("CommitPrefEdits")
/* package */ class PersistentIdentity {

  // Should ONLY be called from an OnPrefsLoadedListener (since it should NEVER be called concurrently)
  public static JSONArray waitingPeopleRecordsForSending(SharedPreferences storedPreferences) {
    JSONArray ret = null;
    final String peopleDistinctId = storedPreferences.getString("people_distinct_id", null);
    final String waitingPeopleRecords = storedPreferences.getString("waiting_array", null);
    if ((null != waitingPeopleRecords) && (null != peopleDistinctId)) {
      JSONArray waitingObjects = null;
      try {
        waitingObjects = new JSONArray(waitingPeopleRecords);
      } catch (final JSONException e) {
        Log.e(LOGTAG, "Waiting people records were unreadable.");
        return null;
      }

      ret = new JSONArray();
      for (int i = 0; i < waitingObjects.length(); i++) {
        try {
          final JSONObject ob = waitingObjects.getJSONObject(i);
          ob.put("$distinct_id", peopleDistinctId);
          ret.put(ob);
        } catch (final JSONException e) {
          Log.e(LOGTAG, "Unparsable object found in waiting people records", e);
        }
      }

      final SharedPreferences.Editor editor = storedPreferences.edit();
      editor.remove("waiting_array");
      writeEdits(editor);
    }
    return ret;
  }

  public static void writeReferrerPrefs(Context context, String preferencesName,
      Map<String, String> properties) {
    synchronized (sReferrerPrefsLock) {
      final SharedPreferences referralInfo =
          context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE);
      final SharedPreferences.Editor editor = referralInfo.edit();
      editor.clear();
      for (final Map.Entry<String, String> entry : properties.entrySet()) {
        editor.putString(entry.getKey(), entry.getValue());
      }
      writeEdits(editor);
      sReferrerPrefsDirty = true;
    }
  }

  public PersistentIdentity(Future<SharedPreferences> referrerPreferences,
      Future<SharedPreferences> storedPreferences) {
    mLoadReferrerPreferences = referrerPreferences;
    mLoadStoredPreferences = storedPreferences;
    mSuperPropertiesCache = null;
    mReferrerPropertiesCache = null;
    mIdentitiesLoaded = false;
    mReferrerChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
      @Override
      public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        synchronized (sReferrerPrefsLock) {
          readReferrerProperties();
          sReferrerPrefsDirty = false;
        }
      }
    };
  }

  public synchronized void addSuperPropertiesToObject(JSONObject ob) {
    final JSONObject superProperties = this.getSuperPropertiesCache();
    final Iterator<?> superIter = superProperties.keys();
    while (superIter.hasNext()) {
      final String key = (String) superIter.next();

      try {
        ob.put(key, superProperties.get(key));
      } catch (JSONException e) {
        Log.wtf(LOGTAG, "Object read from one JSON Object cannot be written to another", e);
      }
    }
  }

  public synchronized void updateSuperProperties(SuperPropertyUpdate updates) {
    final JSONObject oldPropCache = getSuperPropertiesCache();
    final JSONObject copy = new JSONObject();

    try {
      final Iterator<String> keys = oldPropCache.keys();
      while (keys.hasNext()) {
        final String k = keys.next();
        final Object v = oldPropCache.get(k);
        copy.put(k, v);
      }
    } catch (JSONException e) {
      Log.wtf(LOGTAG, "Can't copy from one JSONObject to another", e);
      return;
    }

    final JSONObject replacementCache = updates.update(copy);
    if (null == replacementCache) {
      Log.w(LOGTAG,
          "An update to SensorsData's super properties returned null, and will have no effect.");
      return;
    }

    mSuperPropertiesCache = replacementCache;
    storeSuperProperties();
  }

  public Map<String, String> getReferrerProperties() {
    synchronized (sReferrerPrefsLock) {
      if (sReferrerPrefsDirty || null == mReferrerPropertiesCache) {
        readReferrerProperties();
        sReferrerPrefsDirty = false;
      }
    }
    return mReferrerPropertiesCache;
  }

  public synchronized String getEventsDistinctId() {
    if (!mIdentitiesLoaded) {
      readIdentities();
    }
    return mEventsDistinctId;
  }

  public synchronized void setEventsDistinctId(String eventsDistinctId) {
    if (!mIdentitiesLoaded) {
      readIdentities();
    }
    mEventsDistinctId = eventsDistinctId;
    writeIdentities();
  }

  public synchronized String getPeopleDistinctId() {
    if (!mIdentitiesLoaded) {
      readIdentities();
    }
    return mPeopleDistinctId;
  }

  public synchronized void setPeopleDistinctId(String peopleDistinctId) {
    if (!mIdentitiesLoaded) {
      readIdentities();
    }
    mPeopleDistinctId = peopleDistinctId;
    writeIdentities();
  }

  public synchronized void storeWaitingPeopleRecord(JSONObject record) {
    if (!mIdentitiesLoaded) {
      readIdentities();
    }
    if (null == mWaitingPeopleRecords) {
      mWaitingPeopleRecords = new JSONArray();
    }
    mWaitingPeopleRecords.put(record);
    writeIdentities();
  }

  public synchronized JSONArray waitingPeopleRecordsForSending() {
    JSONArray ret = null;
    try {
      final SharedPreferences prefs = mLoadStoredPreferences.get();
      ret = waitingPeopleRecordsForSending(prefs);
      readIdentities();
    } catch (final ExecutionException e) {
      Log.e(LOGTAG, "Couldn't read waiting people records from shared preferences.", e.getCause());
    } catch (final InterruptedException e) {
      Log.e(LOGTAG, "Couldn't read waiting people records from shared preferences.", e);
    }
    return ret;
  }

  public synchronized void clearPreferences() {
    // Will clear distinct_ids, superProperties,
    // and waiting People Analytics properties. Will have no effect
    // on messages already queued to send with AnalyticsMessages.

    try {
      final SharedPreferences prefs = mLoadStoredPreferences.get();
      final SharedPreferences.Editor prefsEdit = prefs.edit();
      prefsEdit.clear();
      writeEdits(prefsEdit);
      readSuperProperties();
      readIdentities();
    } catch (final ExecutionException e) {
      throw new RuntimeException(e.getCause());
    } catch (final InterruptedException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public synchronized void registerSuperProperties(JSONObject superProperties) {
    final JSONObject propCache = getSuperPropertiesCache();

    for (final Iterator<?> iter = superProperties.keys(); iter.hasNext(); ) {
      final String key = (String) iter.next();
      try {
        propCache.put(key, superProperties.get(key));
      } catch (final JSONException e) {
        Log.e(LOGTAG, "Exception registering super property.", e);
      }
    }

    storeSuperProperties();
  }

  public synchronized void unregisterSuperProperty(String superPropertyName) {
    final JSONObject propCache = getSuperPropertiesCache();
    propCache.remove(superPropertyName);

    storeSuperProperties();
  }

  public synchronized void registerSuperPropertiesOnce(JSONObject superProperties) {
    final JSONObject propCache = getSuperPropertiesCache();

    for (final Iterator<?> iter = superProperties.keys(); iter.hasNext(); ) {
      final String key = (String) iter.next();
      if (!propCache.has(key)) {
        try {
          propCache.put(key, superProperties.get(key));
        } catch (final JSONException e) {
          Log.e(LOGTAG, "Exception registering super property.", e);
        }
      }
    }// for

    storeSuperProperties();
  }

  public synchronized void clearSuperProperties() {
    mSuperPropertiesCache = new JSONObject();
    storeSuperProperties();
  }

  //////////////////////////////////////////////////

  // Must be called from a synchronized setting
  private JSONObject getSuperPropertiesCache() {
    if (null == mSuperPropertiesCache) {
      readSuperProperties();
    }
    return mSuperPropertiesCache;
  }

  // All access should be synchronized on this
  private void readSuperProperties() {
    try {
      final SharedPreferences prefs = mLoadStoredPreferences.get();
      final String props = prefs.getString("super_properties", "{}");
      if (SSConfig.DEBUG) {
        Log.v(LOGTAG, "Loading Super Properties " + props);
      }
      mSuperPropertiesCache = new JSONObject(props);
    } catch (final ExecutionException e) {
      Log.e(LOGTAG, "Cannot load superProperties from SharedPreferences.", e.getCause());
    } catch (final InterruptedException e) {
      Log.e(LOGTAG, "Cannot load superProperties from SharedPreferences.", e);
    } catch (final JSONException e) {
      Log.e(LOGTAG, "Cannot parse stored superProperties");
      storeSuperProperties();
    } finally {
      if (null == mSuperPropertiesCache) {
        mSuperPropertiesCache = new JSONObject();
      }
    }
  }

  // All access should be synchronized on this
  private void readReferrerProperties() {
    mReferrerPropertiesCache = new HashMap<String, String>();

    try {
      final SharedPreferences referrerPrefs = mLoadReferrerPreferences.get();
      referrerPrefs.unregisterOnSharedPreferenceChangeListener(mReferrerChangeListener);
      referrerPrefs.registerOnSharedPreferenceChangeListener(mReferrerChangeListener);

      final Map<String, ?> prefsMap = referrerPrefs.getAll();
      for (final Map.Entry<String, ?> entry : prefsMap.entrySet()) {
        final String prefsName = entry.getKey();
        final Object prefsVal = entry.getValue();
        mReferrerPropertiesCache.put(prefsName, prefsVal.toString());
      }
    } catch (final ExecutionException e) {
      Log.e(LOGTAG, "Cannot load referrer properties from shared preferences.", e.getCause());
    } catch (final InterruptedException e) {
      Log.e(LOGTAG, "Cannot load referrer properties from shared preferences.", e);
    }
  }

  // All access should be synchronized on this
  private void storeSuperProperties() {
    if (null == mSuperPropertiesCache) {
      Log.e(LOGTAG,
          "storeSuperProperties should not be called with uninitialized superPropertiesCache.");
      return;
    }

    final String props = mSuperPropertiesCache.toString();
    if (SSConfig.DEBUG) {
      Log.v(LOGTAG, "Storing Super Properties " + props);
    }

    try {
      final SharedPreferences prefs = mLoadStoredPreferences.get();
      final SharedPreferences.Editor editor = prefs.edit();
      editor.putString("super_properties", props);
      writeEdits(editor);
    } catch (final ExecutionException e) {
      Log.e(LOGTAG, "Cannot store superProperties in shared preferences.", e.getCause());
    } catch (final InterruptedException e) {
      Log.e(LOGTAG, "Cannot store superProperties in shared preferences.", e);
    }
  }

  // All access should be synchronized on this
  private void readIdentities() {
    SharedPreferences prefs = null;
    try {
      prefs = mLoadStoredPreferences.get();
    } catch (final ExecutionException e) {
      Log.e(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e.getCause());
    } catch (final InterruptedException e) {
      Log.e(LOGTAG, "Cannot read distinct ids from sharedPreferences.", e);
    }

    if (null == prefs) {
      return;
    }

    mEventsDistinctId = prefs.getString("events_distinct_id", null);
    mPeopleDistinctId = prefs.getString("people_distinct_id", null);
    mWaitingPeopleRecords = null;

    final String storedWaitingRecord = prefs.getString("waiting_array", null);
    if (storedWaitingRecord != null) {
      try {
        mWaitingPeopleRecords = new JSONArray(storedWaitingRecord);
      } catch (final JSONException e) {
        Log.e(LOGTAG, "Could not interpret waiting people JSON record " + storedWaitingRecord);
      }
    }

    if (null == mEventsDistinctId) {
      mEventsDistinctId = UUID.randomUUID().toString();
      writeIdentities();
    }

    mIdentitiesLoaded = true;
  }

  // All access should be synchronized on this
  private void writeIdentities() {
    try {
      final SharedPreferences prefs = mLoadStoredPreferences.get();
      final SharedPreferences.Editor prefsEditor = prefs.edit();

      prefsEditor.putString("events_distinct_id", mEventsDistinctId);
      prefsEditor.putString("people_distinct_id", mPeopleDistinctId);
      if (mWaitingPeopleRecords == null) {
        prefsEditor.remove("waiting_array");
      } else {
        prefsEditor.putString("waiting_array", mWaitingPeopleRecords.toString());
      }
      writeEdits(prefsEditor);
    } catch (final ExecutionException e) {
      Log.e(LOGTAG, "Can't write distinct ids to shared preferences.", e.getCause());
    } catch (final InterruptedException e) {
      Log.e(LOGTAG, "Can't write distinct ids to shared preferences.", e);
    }
  }

  @TargetApi(Build.VERSION_CODES.GINGERBREAD)
  private static void writeEdits(final SharedPreferences.Editor editor) {
    if (Build.VERSION.SDK_INT >= 9) {
      editor.apply();
    } else {
      editor.commit();
    }
  }

  private final Future<SharedPreferences> mLoadStoredPreferences;
  private final Future<SharedPreferences> mLoadReferrerPreferences;
  private final SharedPreferences.OnSharedPreferenceChangeListener mReferrerChangeListener;
  private JSONObject mSuperPropertiesCache;
  private Map<String, String> mReferrerPropertiesCache;
  private boolean mIdentitiesLoaded;
  private String mEventsDistinctId;
  private String mPeopleDistinctId;
  private JSONArray mWaitingPeopleRecords;

  private static boolean sReferrerPrefsDirty = true;
  private static final Object sReferrerPrefsLock = new Object();
  private static final String LOGTAG = "SA.PersistentIdentity";
}
