package com.sensorsdata.analytics.android.sdkdemo;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ExampleListActivity extends ListActivity {
  private MatrixCursor mCursor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activty_example_list);

    ListView listView = getListView();
    mCursor = new MatrixCursor(new String[] {"_id", "image", "summary"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "1"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "2"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "3"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "4"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "5"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "6"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "7"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "8"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "9"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "10"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "11"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "12"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "13"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "14"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "15"});
    mCursor.addRow(new Object[] {0, R.mipmap.ic_icon, "16"});

    ListAdapter listAdapter = new ExampleListAdapter(this, mCursor);
    setListAdapter(listAdapter);

    //listView.setOnItemClickListener(mNewMacAddress);
    //listView.setOnCreateContextMenuListener(mConvListOnCreateContextMenuListener);
    //listView.setOnKeyListener(mThreadListKeyListener);
    //listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    //listView.setMultiChoiceModeListener(new ModeCallback());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_example_list, menu);
    return true;
  }

  public class ExampleListAdapter extends CursorAdapter {
    private final LayoutInflater mFactory;
    //private OnContentChangedListener mOnContentChangedListener;

    public ExampleListAdapter(Context context, Cursor cursor) {
      super(context, cursor, false /* auto-requery */);
      mFactory = LayoutInflater.from(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
      Log.v("ExampleListAdapter", "inflating new view");
      return mFactory.inflate(R.layout.example_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
      if (!(view instanceof ExampleListItem)) {
        return;
      }

      ExampleListItem headerView = (ExampleListItem) view;
      Long ID = cursor.getLong(0);
      int imageId = cursor.getInt(1);
      String text = cursor.getString(2);
      headerView.bind(context, imageId, text);
    }
  }
}
