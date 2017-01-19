package ru.bmstu.iu6.simplenote.data.database;

import android.content.Context;
import android.util.Base64;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

/**
 * Created by Михаил on 25.12.2016.
 */

class NotesDBOpenHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "Notes.db";
    // Password is used by default if user decides not to encrypt his data.
    // It's just a dummy password to unify and simplify the code and architecture of the app
    // and not to encrypt the DB by default without having to care about DBs
    // without a password set.
    // E.g. see http://stackoverflow.com/questions/19499289/sqlcipher-changing-db-password-failing
    // Otherwise it could've been a headache.
    public static final String DATABASE_DEFAULT_PASSWORD = "abcd1234";

    private static final String DATABSE_SET_KEY = "PRAGMA key = \"%s\"";

    NotesDBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // use base64 to prevent injections
        final String base64Pass =
                Base64.encodeToString(DATABASE_DEFAULT_PASSWORD.getBytes(),
                        Base64.DEFAULT);
        sqLiteDatabase.execSQL(String.format(DATABSE_SET_KEY, base64Pass));

        sqLiteDatabase.execSQL(NotesContract.CREATE_TABLE);
        sqLiteDatabase.execSQL(NotesContract.CREATE_FTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        dropDatabase(sqLiteDatabase);
        onCreate(sqLiteDatabase);
    }

    private void dropDatabase(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(NotesContract.DROP_TABLE);
        sqLiteDatabase.execSQL(NotesContract.DROP_FTS_TABLE);
    }
}
