package ru.bmstu.iu6.simplenote.loaders;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.List;

import ru.bmstu.iu6.simplenote.database.NotesDAO;
import ru.bmstu.iu6.simplenote.models.Note;

/**
 * Created by Михаил on 26.12.2016.
 */

public class NotesLoader extends AsyncTaskLoader<List<Note>> {
    public NotesLoader(@NonNull Context context) {
        super(context);
    }

    @Override
    public List<Note> loadInBackground() {
        NotesDAO dao = new NotesDAO(getContext());
        List<Note> data;
        try {
            dao.open();
            data = dao.getNotes();
        } finally {
            dao.close();
        }
        return data;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }
}
