package com.chris_cartwright.android.bicyclemonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class DbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "History.db";
    public static final String ID_NAME = "id";
    public static final String SPEED_NAME = "speed";
    public static final String CADENCE_NAME = "cadence";
    public static final String CREATED_NAME = "created";
    public static final String UUID_NAME = "uuid";
    public static final String TABLE_NAME = "history";

    private static final String SQL_CREATE_ENTRIES =
        "CREATE TABLE " + TABLE_NAME + " (" +
            ID_NAME + " INTEGER PRIMARY KEY, " +
            SPEED_NAME + " DECIMAL(5,2), " +
            CADENCE_NAME + " INT, " +
            UUID_NAME + " VARCHAR(36), " +
            CREATED_NAME + " BIGINT DEFAULT (STRFTIME('%s', 'now'))" +
        ")";

    private static final String SQL_DELETE_ENTRIES =
        "DROP TABLE IF EXISTS " + TABLE_NAME;

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
        values.put(DbHelper.UUID_NAME, entry.getUuid().toString());
        SQLiteDatabase db = DbHelper.this.getWritableDatabase();
        db.insert(DbHelper.TABLE_NAME, null, values);
    }

    public HistoryEntry[] get() {
        SQLiteDatabase db = DbHelper.this.getReadableDatabase();
        Cursor cur = db.query(
            TABLE_NAME,
            new String[] { SPEED_NAME, CADENCE_NAME, UUID_NAME, CREATED_NAME },
            null,
            null,
            null,
            null,
            CREATED_NAME + " DESC",
            "10"
        );

        ArrayList<HistoryEntry> ret = new ArrayList<>();
        while(cur.moveToNext()) {
            HistoryEntry entry = new HistoryEntry();
            entry.setSpeed(cur.getDouble(0));
            entry.setCadence(cur.getInt(1));
            entry.setUuid(UUID.fromString(cur.getString(2)));
            entry.setCreated(new Date(cur.getLong(3)));
            ret.add(entry);
        }

        cur.close();
        db.close();

        return ret.toArray(new HistoryEntry[ret.size()]);
    }
}
