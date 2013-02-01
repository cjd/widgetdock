package com.adebenham.widgetdock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class WidgIdProvider {
	static final String TAG = "WidgetDock";

	// Database fields
	private SQLiteDatabase database;
	private MySQLiteHelper dbHelper;
	private String[] allColumns = { MySQLiteHelper.COLUMN_ID,
			MySQLiteHelper.COLUMN_WID, MySQLiteHelper.COLUMN_NAME };

	public WidgIdProvider(Context context) {
		dbHelper = new MySQLiteHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public WidgInfo addWidget(long wid, String name) {
		ContentValues values = new ContentValues();
		values.put(MySQLiteHelper.COLUMN_WID, wid);
		values.put(MySQLiteHelper.COLUMN_NAME, name);
		long insertId = database.insert(MySQLiteHelper.TABLE_WIDGETS, null,
				values);
		Cursor cursor = database.query(MySQLiteHelper.TABLE_WIDGETS,
				allColumns, MySQLiteHelper.COLUMN_ID + " = " + insertId, null,
				null, null, null);

		cursor.moveToFirst();
		WidgInfo newWidget = cursorToWidgInfo(cursor);
		cursor.close();
		Log.d(TAG, "widget added and written to DB: " + name);
		return newWidget;
	}

	public WidgInfo cursorToWidgInfo(Cursor cursor) {
		WidgInfo widget = new WidgInfo();
		widget.setId(cursor.getLong(0));
		widget.setWid(cursor.getInt(1));
		widget.setName(cursor.getString(2));
		return widget;
	}

	public void removeWidget(int wid) {
		database.delete(MySQLiteHelper.TABLE_WIDGETS, MySQLiteHelper.COLUMN_WID
				+ "=" + wid, null);
	}
	
	public Cursor getAllWidgInfo() {
		Cursor cursor = database.query(MySQLiteHelper.TABLE_WIDGETS, allColumns,null,null,null,null,null);
		cursor.moveToFirst();
		return cursor;
	}
}
