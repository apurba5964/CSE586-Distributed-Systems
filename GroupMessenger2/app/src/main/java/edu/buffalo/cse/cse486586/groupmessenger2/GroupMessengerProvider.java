package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */

/**
 * GroupMessengerProviderDatabase is the SQLite database for the assignment which stores the
 * key and value pairs.
 *
 * @author apurbama
 * Reference : https://developer.android.com/training/data-storage/sqlite
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    private static final String TAG = "GroupMessengerProvider";
    private GroupMessengerProviderDatabase contentProviderDB;
    private static final String TABLE_NAME = "GroupMessenger";


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         *
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */

        SQLiteDatabase db = contentProviderDB.getWritableDatabase();
        String [] selectionArguments = {values.getAsString("key")};

        Cursor cursor = db.query(TABLE_NAME,null,"key = ?",selectionArguments,null,null,null);
        if(cursor.getCount()<1) {
            db.insert(TABLE_NAME, null, values);
        } else {
            db.update(TABLE_NAME,values,"key = ?",selectionArguments);
        }





        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        contentProviderDB = new GroupMessengerProviderDatabase(getContext(),null,null,1);
        Log.v(TAG,"Content Provider DB creation successful");
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        Log.v("query", selection);
        SQLiteDatabase queryDB = contentProviderDB.getReadableDatabase();
        String[] arguments = {selection};
        Cursor cursor = queryDB.query(TABLE_NAME,null,"key = ?",arguments,null,null
                ,null);

        if(cursor.getCount()<1){
            Log.e(TAG,"Wrong key provided.No data found in database");
            return null;
        }


        return cursor;
    }
}
