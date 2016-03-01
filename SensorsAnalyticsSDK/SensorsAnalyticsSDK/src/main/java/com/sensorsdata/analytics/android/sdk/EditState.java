package com.sensorsdata.analytics.android.sdk;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles applying and managing the life cycle of edits in an application. Clients
 * can replace all of the edits in an app with {@link EditState#setEdits(java.util.Map)}.
 * <p>
 * Some client is responsible for informing the EditState about the presence or absence
 * of Activities, by calling {@link EditState#add(android.app.Activity)} and {@link EditState#remove(android.app.Activity)}
 */
public class EditState extends UIThreadSet<Activity> {

  public EditState() {
    mUiThreadHandler = new Handler(Looper.getMainLooper());
    mIntendedEdits = new HashMap<String, List<ViewVisitor>>();
    mCurrentEdits = new HashSet<EditBinding>();
  }

  /**
   * Should be called whenever a new Activity appears in the application.
   */
  @Override
  public void add(Activity newOne) {
    super.add(newOne);
    applyEditsOnUiThread();
  }

  /**
   * Should be called whenever an activity leaves the application, or is otherwise no longer relevant to our edits.
   */
  @Override
  public void remove(Activity oldOne) {
    super.remove(oldOne);
  }

  /**
   * Sets the entire set of edits to be applied to the application.
   * <p>
   * Edits are represented by ViewVisitors, batched in a map by the String name of the activity
   * they should be applied to. Edits to apply to all views should be in a list associated with
   * the key {@code null} (Not the string "null", the actual null value!)
   * <p>
   * The given edits will completely replace any existing edits.
   * <p>
   * setEdits can be called from any thread, although the changes will occur (eventually) on the
   * UI thread of the application, and may not appear immediately.
   *
   * @param newEdits A Map from activity name to a list of edits to apply
   */
  // Must be thread-safe
  public void setEdits(Map<String, List<ViewVisitor>> newEdits) {
    // Delete images that are no longer needed

    synchronized (mCurrentEdits) {
      for (final EditBinding stale : mCurrentEdits) {
        stale.kill();
      }
      mCurrentEdits.clear();
    }

    synchronized (mIntendedEdits) {
      mIntendedEdits.clear();
      mIntendedEdits.putAll(newEdits);
    }

    applyEditsOnUiThread();
  }

  private void applyEditsOnUiThread() {
    if (Thread.currentThread() == mUiThreadHandler.getLooper().getThread()) {
      applyIntendedEdits();
    } else {
      mUiThreadHandler.post(new Runnable() {
        @Override
        public void run() {
          applyIntendedEdits();
        }
      });
    }
  }

  // Must be called on UI Thread
  private void applyIntendedEdits() {
    for (final Activity activity : getAll()) {
      final String activityName = activity.getClass().getCanonicalName();
      final View rootView = activity.getWindow().getDecorView().getRootView();

      final List<ViewVisitor> specificChanges;
      final List<ViewVisitor> wildcardChanges;
      synchronized (mIntendedEdits) {
        specificChanges = mIntendedEdits.get(activityName);
        wildcardChanges = mIntendedEdits.get(null);
      }

      if (null != specificChanges) {
        applyChangesFromList(rootView, specificChanges);
      }

      if (null != wildcardChanges) {
        applyChangesFromList(rootView, wildcardChanges);
      }
    }
  }

  // Must be called on UI Thread
  private void applyChangesFromList(View rootView, List<ViewVisitor> changes) {
    synchronized (mCurrentEdits) {
      final int size = changes.size();
      for (int i = 0; i < size; i++) {
        final ViewVisitor visitor = changes.get(i);
        final EditBinding binding = new EditBinding(rootView, visitor, mUiThreadHandler);
        mCurrentEdits.add(binding);
      }
    }
  }

  /* The binding between a bunch of edits and a view. Should be instantiated and live on the UI thread */
  private static class EditBinding implements ViewTreeObserver.OnGlobalLayoutListener, Runnable {
    public EditBinding(View viewRoot, ViewVisitor edit, Handler uiThreadHandler) {
      mEdit = edit;
      mViewRoot = new WeakReference<View>(viewRoot);
      mHandler = uiThreadHandler;
      mAlive = true;
      mDying = false;

      final ViewTreeObserver observer = viewRoot.getViewTreeObserver();
      if (observer.isAlive()) {
        observer.addOnGlobalLayoutListener(this);
      }
      run();
    }

    @Override
    public void onGlobalLayout() {
      run();
    }

    @Override
    public void run() {
      if (!mAlive) {
        return;
      }

      final View viewRoot = mViewRoot.get();
      if (null == viewRoot || mDying) {
        cleanUp();
        return;
      }
      // ELSE View is alive and we are alive

      mEdit.visit(viewRoot);
      mHandler.removeCallbacks(this);
      mHandler.postDelayed(this, 1000);
    }

    public void kill() {
      mDying = true;
      mHandler.post(this);
    }

    @SuppressWarnings("deprecation")
    private void cleanUp() {
      if (mAlive) {
        final View viewRoot = mViewRoot.get();
        if (null != viewRoot) {
          final ViewTreeObserver observer = viewRoot.getViewTreeObserver();
          if (observer.isAlive()) {
            observer.removeGlobalOnLayoutListener(this); // Deprecated Name
          }
        }
        mEdit.cleanup();
      }
      mAlive = false;
    }

    private volatile boolean mDying;
    private boolean mAlive;
    private final WeakReference<View> mViewRoot;
    private final ViewVisitor mEdit;
    private final Handler mHandler;
  }


  private final Handler mUiThreadHandler;
  private final Map<String, List<ViewVisitor>> mIntendedEdits;
  private final Set<EditBinding> mCurrentEdits;

  private static final String LOGTAG = "SA.EditState";
}
