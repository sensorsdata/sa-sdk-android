package com.sensorsdata.analytics.android.sdk;

import com.sensorsdata.analytics.android.sdk.exceptions.InvalidDataException;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class DynamicEventTracker implements ViewVisitor.OnEventListener {

  public DynamicEventTracker(Context context, Handler homeHandler) {
    mContext = context;
    mDebouncedEvents = new HashMap<Signature, UnsentEvent>();
    mTask = new SendDebouncedTask();
    mHandler = homeHandler;
  }

  @Override
  public void OnEvent(View v, EventInfo eventInfo, boolean debounce) {
    final long moment = System.currentTimeMillis();

    final JSONObject properties = new JSONObject();
    try {
      properties.put("$from_vtrack", String.valueOf(eventInfo.mTriggerId));
      properties.put("$binding_trigger_id", eventInfo.mTriggerId);
      properties.put("$binding_path", eventInfo.mPath);
      properties.put("$binding_depolyed", eventInfo.mIsDeployed);
    } catch (JSONException e) {
      Log.e(LOGTAG, "Can't format properties from view due to JSON issue", e);
    }

    if (SensorsDataAPI.sharedInstance(mContext).isDebugMode()) {
      Log.v(LOGTAG, String.format("VTrack event occured. [event_name='%s' event_type='%s' "
          + "debounce='%s' properties='%s']", eventInfo.mEventName, eventInfo.mEventType,
          debounce, properties.toString()));
    }

    // 对于Clicked事件，事件发生时即调用track记录事件；对于Edited事件，由于多次Edit时会触发多次Edited，
    // 所以我们增加一个计时器，延迟发送Edited事件
    if (debounce) {
      final Signature eventSignature = new Signature(v, eventInfo);
      final UnsentEvent event = new UnsentEvent(eventInfo, properties, moment);

      // No scheduling mTask without holding a lock on mDebouncedEvents,
      // so that we don't have a rogue thread spinning away when no events
      // are coming in.
      synchronized (mDebouncedEvents) {
        final boolean needsRestart = mDebouncedEvents.isEmpty();
        mDebouncedEvents.put(eventSignature, event);
        if (needsRestart) {
          mHandler.postDelayed(mTask, DEBOUNCE_TIME_MILLIS);
        }
      }
    } else {
      try {
        SensorsDataAPI.sharedInstance(mContext).track(eventInfo.mEventName, properties);
      } catch (InvalidDataException e) {
        Log.w("Unexpected exception", e);
      }
    }
  }

  // Attempts to send all tasks in mDebouncedEvents that have been waiting for
  // more than DEBOUNCE_TIME_MILLIS. Will reschedule itself as long as there
  // are more events waiting (but will *not* wait on an empty set)
  private final class SendDebouncedTask implements Runnable {
    @Override
    public void run() {
      final long now = System.currentTimeMillis();
      synchronized (mDebouncedEvents) {
        final Iterator<Map.Entry<Signature, UnsentEvent>> iter =
            mDebouncedEvents.entrySet().iterator();
        while (iter.hasNext()) {
          final Map.Entry<Signature, UnsentEvent> entry = iter.next();
          final UnsentEvent val = entry.getValue();
          if (now - val.timeSentMillis > DEBOUNCE_TIME_MILLIS) {
            try {
              SensorsDataAPI.sharedInstance(mContext)
                  .track(val.eventInfo.mEventName, val.properties);
            } catch (InvalidDataException e) {
              Log.w("Unexpected exception", e);
            }
            iter.remove();
          }
        }

        if (!mDebouncedEvents.isEmpty()) {
          // In the average case, this is enough time to catch the next signal
          mHandler.postDelayed(this, DEBOUNCE_TIME_MILLIS / 2);
        }
      } // synchronized
    }
  }

  /**
   * Recursively scans a view and it's children, looking for user-visible text to
   * provide as an event property.
   */
  private static String textPropertyFromView(View v) {
    String ret = null;

    if (v instanceof TextView) {
      final TextView textV = (TextView) v;
      final CharSequence retSequence = textV.getText();
      if (null != retSequence) {
        ret = retSequence.toString();
      }
    } else if (v instanceof ViewGroup) {
      final StringBuilder builder = new StringBuilder();
      final ViewGroup vGroup = (ViewGroup) v;
      final int childCount = vGroup.getChildCount();
      boolean textSeen = false;
      for (int i = 0; i < childCount && builder.length() < MAX_PROPERTY_LENGTH; i++) {
        final View child = vGroup.getChildAt(i);
        final String childText = textPropertyFromView(child);
        if (null != childText && childText.length() > 0) {
          if (textSeen) {
            builder.append(", ");
          }
          builder.append(childText);
          textSeen = true;
        }
      }

      if (builder.length() > MAX_PROPERTY_LENGTH) {
        ret = builder.substring(0, MAX_PROPERTY_LENGTH);
      } else if (textSeen) {
        ret = builder.toString();
      }
    }

    return ret;
  }

  // An event is the same from a debouncing perspective if it comes from the same view,
  // and has the same event name.
  private static class Signature {
    public Signature(final View view, final EventInfo eventInfo) {
      mHashCode = view.hashCode() ^
          eventInfo.mEventName.hashCode() ^
          String.valueOf(eventInfo.mTriggerId).hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Signature) {
        return mHashCode == o.hashCode();
      }

      return false;
    }

    @Override
    public int hashCode() {
      return mHashCode;
    }

    private final int mHashCode;
  }

  private static class UnsentEvent {
    public UnsentEvent(final EventInfo event, final JSONObject props, final long timeSent) {
      eventInfo = event;
      properties = props;
      timeSentMillis = timeSent;
    }

    public final long timeSentMillis;
    public final EventInfo eventInfo;
    public final JSONObject properties;
  }

  private final Context mContext;
  private final Handler mHandler;
  private final Runnable mTask;

  // List of debounced events, All accesses must be synchronized
  private final Map<Signature, UnsentEvent> mDebouncedEvents;

  private static final int MAX_PROPERTY_LENGTH = 128;
  private static final int DEBOUNCE_TIME_MILLIS = 1000; // 1 second delay before sending

  private static String LOGTAG = "SA.DynamicEventTracker";
}
