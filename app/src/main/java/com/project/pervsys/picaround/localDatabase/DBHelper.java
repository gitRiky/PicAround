package com.project.pervsys.picaround.localDatabase;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.project.pervsys.picaround.utility.Config.ID;
import static com.project.pervsys.picaround.utility.Config.USERNAME;
import static com.project.pervsys.picaround.utility.Config.USERNAMES;

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "PicAround";
    public static final String CREATE_TABLE_USERNAMES =
            "CREATE TABLE IF NOT EXISTS " + USERNAMES + " (" + USERNAME + " TEXT PRIMARY KEY," +
                    ID + " TEXT )";
    public static final String DELETE_TABLE_USERNAMES = "DROP TABLE IF EXISTS " + USERNAMES;

    public DBHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate()");
        db.execSQL(CREATE_TABLE_USERNAMES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

}
