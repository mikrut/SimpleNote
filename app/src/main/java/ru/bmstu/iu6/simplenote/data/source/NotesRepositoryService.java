package ru.bmstu.iu6.simplenote.data.source;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ru.bmstu.iu6.simplenote.data.database.NotesDAO;
import ru.bmstu.iu6.simplenote.models.INote;

/**
 * Created by Михаил on 27.12.2016.
 */

public class NotesRepositoryService extends Service {
    private NotesDataSource notesDataSource;

    private final BlockingQueue<Runnable> mRepositoryWorkQueue = new LinkedBlockingQueue<>();
    private ThreadPoolExecutor repositoryThreadPool;
    private static final int NUMBER_OF_THREADS = 3;
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    public interface NotesRepositoryObserver {
        void onGetNotes(List<? extends INote> notes);
        void onGetNote(INote note);
        void onSaveNoteResult(long result);
        void onDeleteFinish();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new NotesRepository(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.notesDataSource = NotesDAO.getInstance(getApplicationContext());

        repositoryThreadPool = new ThreadPoolExecutor(
                NUMBER_OF_THREADS,
                NUMBER_OF_THREADS * 2,
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                mRepositoryWorkQueue
        );
    }

    private void getNotesAsync(NotesRepositoryObserver observer) {
        List<? extends INote> notes = notesDataSource.getNotes();
        if (observer != null)
            observer.onGetNotes(notes);
    }

    public void getNotes(final NotesRepositoryObserver observer) {
        repositoryThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                getNotesAsync(observer);
            }
        });
    }

    private void saveNoteAsync(@NonNull INote note, NotesRepositoryObserver observer) {
        long nid = notesDataSource.saveNote(note);
        if (observer != null)
            observer.onSaveNoteResult(nid);
    }

    public void saveNote(@NonNull final INote note, final NotesRepositoryObserver observer) {
        repositoryThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                saveNoteAsync(note, observer);
            }
        });
    }

    private void deleteNotesAsync(@NonNull Set<Integer> nids, NotesRepositoryObserver observer) {
        notesDataSource.deleteNotes(nids);
        if (observer != null)
            observer.onDeleteFinish();
    }

    public void deleteNotes(@NonNull final Set<Integer> nids, final NotesRepositoryObserver observer) {
        repositoryThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                deleteNotesAsync(nids, observer);
            }
        });
    }

    private void getNoteAsync(int nid, NotesRepositoryObserver observer) {
        INote note = notesDataSource.getNote(nid);
        if (observer != null)
            observer.onGetNote(note);
    }

    public void getNote(final int nid, final NotesRepositoryObserver observer) {
        repositoryThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                getNoteAsync(nid, observer);
            }
        });
    }
}
