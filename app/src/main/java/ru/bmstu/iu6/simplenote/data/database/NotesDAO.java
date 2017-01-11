package ru.bmstu.iu6.simplenote.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import ru.bmstu.iu6.simplenote.data.source.NotesDataSource;
import ru.bmstu.iu6.simplenote.models.INote;
import ru.bmstu.iu6.simplenote.models.ISearchNote;
import ru.bmstu.iu6.simplenote.models.Note;
import ru.bmstu.iu6.simplenote.models.SearchNote;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import static org.apache.commons.lang3.StringEscapeUtils.unescapeHtml4;

/**
 * Created by Михаил on 25.12.2016.
 */

public class NotesDAO implements NotesDataSource {
    private NotesDBOpenHelper dbHelper;
    private SQLiteDatabase database;

    private static NotesDAO INSTANCE;
    private static final Object INSTANCE_SYNCHRONIZER = new Object();

    private NotesDAO(@NonNull Context context) {
        dbHelper = new NotesDBOpenHelper(context);
        database = dbHelper.getWritableDatabase();
    }

    public static NotesDAO getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (INSTANCE_SYNCHRONIZER) {
                if (INSTANCE == null) {
                    INSTANCE = new NotesDAO(context);
                }
            }
        }
        return INSTANCE;
    }

    public long saveNote(@NonNull INote note) {
        // TODO: consider using transaction

        ContentValues values = getContentValues(note);
        long res = database.insertWithOnConflict(
                NotesContract.NotesEntry.TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );

        ContentValues ftsValues = new ContentValues();
        ftsValues.put(NotesContract.NotesEntry.COLUMN_NAME_TEXT, escapeHtml4(note.getText()));
        ftsValues.put("docid", res);
        database.insertWithOnConflict("fts_" + NotesContract.NotesEntry.TABLE_NAME,
                null, ftsValues, SQLiteDatabase.CONFLICT_REPLACE);

        return res;
    }

    private static String selectionForProjection(String table, String[] projection) {
        StringBuilder queryBuilder = new StringBuilder();
        for (int i = 0; i < projection.length; i++) {
            queryBuilder.append(table + "." + projection[i]);
            if (i != projection.length - 1)
                queryBuilder.append(", ");
        }
        return queryBuilder.toString();
    }

    @NonNull
    public List<? extends ISearchNote> getNotes(@NonNull String queryString) {
        final String[] selectionArgs = { queryString };
        final String[] ftsQuasiProjection = {"docid", "snippet"};
        // TODO: probably should consider this: { queryString + "*", "%" + queryString + "%" }

        final String selectionNotes = selectionForProjection(NotesContract.NotesEntry.TABLE_NAME,
                NotesContract.NOTE_PROJECTION);
        final String selectionFts = selectionForProjection("fts", ftsQuasiProjection);

        Cursor cursor = database.rawQuery(
                "SELECT " + selectionNotes + ", " + selectionFts + " FROM " +
                "(" +
                    "SELECT docid, snippet(fts_" + NotesContract.NotesEntry.TABLE_NAME + ", \'<b>\', \'</b>\', \'...\', -1, -4) AS snippet " +
                    "FROM fts_" + NotesContract.NotesEntry.TABLE_NAME +
                    " WHERE fts_" + NotesContract.NotesEntry.TABLE_NAME + " MATCH ?" +
                ") AS fts LEFT JOIN " + NotesContract.NotesEntry.TABLE_NAME +
                " ON " + "fts.docid = " + NotesContract.NotesEntry.TABLE_NAME + "._id", selectionArgs);

        List<ISearchNote> notes = new ArrayList<>(cursor.getCount());

        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            Note note = createNote(cursor);
            SearchNote searchNote = new SearchNote(note, cursor.getString(NotesContract.NOTE_PROJECTION.length + 1));
            notes.add(searchNote);
        }

        cursor.close();
        return notes;
    }

    @NonNull
    public List<? extends INote> getNotes() {
        final String orderBy = NotesContract.NotesEntry.COLUMN_NAME_DATETIME + " DESC";

        Cursor cursor = database.query(
                NotesContract.NotesEntry.TABLE_NAME,
                NotesContract.NOTE_PROJECTION,
                null, // Return all notes
                null, // No WHERE - no args
                null, // No GROUP BY
                null, // No GROUP BY filter
                orderBy
        );

        return cursorToList(cursor);
    }

    public void deleteNotes(@NonNull Set<Integer> nids) {
        final String where = NotesContract.NotesEntry.COLUMN_NAME_NID + " = ?";
        String[] selectionArgs = new String[1];

        for (int nid: nids) {
            selectionArgs[0] = String.valueOf(nid);
            database.delete(
                    NotesContract.NotesEntry.TABLE_NAME,
                    where,
                    selectionArgs
            );
        }
    }

    @Nullable
    public Note getNote(int nid) {
        final String selection = NotesContract.NotesEntry.COLUMN_NAME_NID + " = ?";
        String[] selectionArgs = { String.valueOf(nid) };

        Cursor cursor = database.query(
                NotesContract.NotesEntry.TABLE_NAME,
                NotesContract.NOTE_PROJECTION,
                selection,
                selectionArgs,
                null,
                null,
                null,
                "1"
        );

        Note note = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            note = createNote(cursor);
        }

        cursor.close();
        return note;
    }

    private static ContentValues getContentValues(INote note) {
        ContentValues values = new ContentValues(NotesContract.NOTE_PROJECTION.length);

        values.put(NotesContract.NotesEntry.COLUMN_NAME_NID, note.getNid());
        values.put(NotesContract.NotesEntry.COLUMN_NAME_DATETIME, note.getDateTime().getTimeInMillis() / 1000L);
        values.put(NotesContract.NotesEntry.COLUMN_NAME_TEXT, note.getText());

        return values;
    }

    private static Note createNote(Cursor cursor) {
        final int nid = cursor.getInt(NotesContract.PROJECTION_NID_INDEX);
        final String text = cursor.getString(NotesContract.PROJECTION_TEXT_INDEX);
        final Calendar dateTime = GregorianCalendar.getInstance();
        dateTime.setTimeInMillis(1000L * cursor.getInt(NotesContract.PROJECTION_DATETIME_INDEX));

        return new Note(nid, text, dateTime);
    }

    private static List<Note> cursorToList(Cursor notesCursor) {
        List<Note> notes = new ArrayList<>(notesCursor.getCount());

        for (notesCursor.moveToFirst(); !notesCursor.isAfterLast(); notesCursor.moveToNext()) {
            Note note = createNote(notesCursor);
            notes.add(note);
        }

        return notes;
    }

}
