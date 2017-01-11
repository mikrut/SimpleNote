package ru.bmstu.iu6.simplenote.data.database;

import android.provider.BaseColumns;

/**
 * Created by Михаил on 25.12.2016.
 */

class NotesContract {
    private static final String COMMA_SEP = ",";

    static final String CREATE_TABLE =
            "CREATE TABLE " + NotesEntry.TABLE_NAME + " (" +
                NotesEntry.COLUMN_NAME_NID  +  " INTEGER PRIMARY KEY" + COMMA_SEP +
                NotesEntry.COLUMN_NAME_TEXT + " TEXT NOT NULL" + COMMA_SEP +
                NotesEntry.COLUMN_NAME_DATETIME + " INTEGER NOT NULL" +
            ")";
    static final String CREATE_FTS_TABLE = "" +
            "CREATE VIRTUAL TABLE fts_" + NotesEntry.TABLE_NAME +
            " USING fts4 (content=\'" + NotesEntry.TABLE_NAME + "\', " +
            NotesEntry.COLUMN_NAME_TEXT + ")";

    static final String DROP_TABLE =
            "DROP TABLE IF EXISTS " + NotesEntry.TABLE_NAME;
    static final String DROP_FTS_TABLE =
            "DROP TABLE IF EXISTS fts_" + NotesEntry.TABLE_NAME;

    static final String[] NOTE_PROJECTION = {
      NotesEntry.COLUMN_NAME_NID,
      NotesEntry.COLUMN_NAME_TEXT,
      NotesEntry.COLUMN_NAME_DATETIME
    };

    static final int PROJECTION_NID_INDEX  = 0;
    static final int PROJECTION_TEXT_INDEX = 1;
    static final int PROJECTION_DATETIME_INDEX = 2;

    static abstract class NotesEntry implements BaseColumns {
        static final String TABLE_NAME = "notes";
        static final String COLUMN_NAME_NID  = "_id";
        static final String COLUMN_NAME_TEXT = "text";
        static final String COLUMN_NAME_DATETIME = "datetime";
    }
}
