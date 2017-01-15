package ru.bmstu.iu6.simplenote.activities.save_file;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ru.bmstu.iu6.simplenote.data.source.NotesDataSource;
import ru.bmstu.iu6.simplenote.models.INote;
import rx.observables.BlockingObservable;

/**
 * Created by Михаил on 05.01.2017.
 */

/**
 * Async task arguments are:
 * <ol>
 * <li> arg 0 - note id (nid), </li>
 * <li> arg 1 - filename (complete canonical path), </li>
 * </ol>
 */
public class SaveFileTask extends AsyncTask<String, Void, Boolean> {
    private NotesDataSource db;
    private SaveFileListener listener;

    interface SaveFileListener {
        void onFileSaved(boolean success);
    }

    public SaveFileTask(NotesDataSource database) {
        this.db = database;
    }

    public void setListener(SaveFileListener listener) {
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        final String nid = strings[0];
        final String filename = strings[1];
        
        INote note = BlockingObservable.from(db.getNote(Integer.valueOf(nid))).firstOrDefault(null);
        if (note == null)
            return false;

        boolean ok = true;
        File f = new File(filename);
        try {
            if (!f.exists())
                f.createNewFile();
            FileWriter writer = new FileWriter(f, false);
            writer.write(note.getText());
            writer.close();
        } catch (IOException ex) {
            ok = false;
            ex.printStackTrace();
        }

        return ok;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        if (listener != null) {
            listener.onFileSaved(aBoolean);
        }
    }
}
