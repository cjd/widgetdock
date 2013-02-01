package com.adebenham.widgetdock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "widgId.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_WIDGETS = "widgets";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_WID = "wid";
    public static final String COLUMN_NAME = "name"; 

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_WIDGETS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_WID + " INTEGER,"
                + COLUMN_NAME + " TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLiteHelper.class.getName(), "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIDGETS);
        onCreate(db);
    }
}
