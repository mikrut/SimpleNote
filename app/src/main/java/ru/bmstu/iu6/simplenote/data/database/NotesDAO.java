package ru.bmstu.iu6.simplenote.data.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
import ru.bmstu.iu6.simplenote.models.Note;

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
        ContentValues values = getContentValues(note);
        return database.insertWithOnConflict(
                NotesContract.NotesEntry.TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
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

    // TODO: methods
}
