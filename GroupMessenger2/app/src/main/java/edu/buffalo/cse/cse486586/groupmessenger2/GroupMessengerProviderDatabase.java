package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * GroupMessengerProviderDatabase is the SQLite database for the assignment which stores the
 * key and value pairs.
 *
 * @author apurbama
 * Reference : https://developer.android.com/training/data-storage/sqlite
 *
 */

public class GroupMessengerProviderDatabase extends SQLiteOpenHelper {


    private static final String DATABASE_NAME = "GroupMessenger.db";
    private static final String TABLE_NAME = "GroupMessenger";
    private static final String CREATE_TABLE = "CREATE TABLE " +TABLE_NAME+" ('key' TEXT, value TEXT)";
    private static final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;


    public GroupMessengerProviderDatabase(Context context, String name,
                                          SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DROP_TABLE);
        db.execSQL(CREATE_TABLE);

        Log.v("db onCreate","GroupMessenger");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL(DROP_TABLE);
        onCreate(db);
        Log.v("db onUpgrade","group_messenger");

    }
}
