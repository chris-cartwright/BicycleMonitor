package com.chris_cartwright.android.bicyclemonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "History.db";
    public static final String ID_NAME = "id";
    public static final String SPEED_NAME = "speed";
    public static final String CADENCE_NAME = "cadence";
    public static final String CREATED_NAME = "created";
    public static final String PACKET_NAME = "packet_num";
    public static final String TABLE_NAME = "history";

    private static final String SQL_CREATE_ENTRIES =
        "CREATE TABLE " + TABLE_NAME + " (" +
            ID_NAME + " INTEGER PRIMARY KEY, " +
            SPEED_NAME + " INT, " +
            CADENCE_NAME + " INT, " +
            PACKET_NAME + " INT, " +
            CREATED_NAME + " DATETIME DEFAULT CURRENT_TIMESTAMP" +
        ")";

    private static final String SQL_DELETE_ENTRIES =
        "DROP TABLE IF EXISTS history";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Just cycle the data
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void add(HistoryEntry entry) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.SPEED_NAME, entry.getSpeed());
        values.put(DbHelper.CADENCE_NAME, entry.getCadence());
        values.put(DbHelper.PACKET_NAME, entry.getPacketNum());
        SQLiteDatabase db = DbHelper.this.getWritableDatabase();
        db.insert(DbHelper.TABLE_NAME, null, values);
    }
}
