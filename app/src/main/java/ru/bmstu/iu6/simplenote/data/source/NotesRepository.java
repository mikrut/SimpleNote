package ru.bmstu.iu6.simplenote.data.source;

import android.os.Binder;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;

import java.util.List;
import java.util.Set;

import ru.bmstu.iu6.simplenote.models.INote;
import ru.bmstu.iu6.simplenote.models.ISearchNote;

/**
 * Created by Михаил on 27.12.2016.
 */

public class NotesRepository
        extends Binder
        implements NotesRepositoryService.NotesRepositoryObserver {
    private final NotesRepositoryService service;
    private NotesRepositoryService.NotesRepositoryObserver observer;
    private Handler handler;

    // TODO: caching stuff
    public NotesRepository(NotesRepositoryService service) {
        this.service = service;
    }

    public void setContentObserver(NotesRepositoryService.NotesRepositoryObserver observer, Handler handler) {
        this.observer = observer;
        this.handler = handler;
    }

    public void removeContentObserver() {
        this.observer = null;
        this.handler = null;
    }

    public void getNotes() {
        service.getNotes(this);
    }

    public void saveNote(@NonNull INote note) {
        service.saveNote(note, this);
    }

    public void deleteNotes(@NonNull Set<Integer> nids) {
        service.deleteNotes(nids, this);
    }

    public void getNote(int nid) {
        service.getNote(nid, this);
    }

    public void getNotes(@NonNull String query) {
        service.getNotes(query, this);
    }

    @Override
    public void onGetNotes(final List<? extends INote> notes) {
        if (observer != null) {
            handler.post(() -> {
                if (observer != null) {
                    observer.onGetNotes(notes);
                }
            });
        }
    }

    @Override
    public void onFindNotesResult(final List<? extends ISearchNote> notes) {
        if (observer != null) {
            handler.post(() -> {
                if (observer != null) {
                    observer.onFindNotesResult(notes);
                }
            });
        }
    }

    @Override
    public void onGetNote(final INote note) {
        if (observer != null) {
            handler.post(() -> {
                if (observer != null) {
                    observer.onGetNote(note);
                }
            });
        }
    }

    @Override
    public void onSaveNoteResult(final long result) {
        if (observer != null) {
            handler.post(() -> {
                if (observer != null) {
                    observer.onSaveNoteResult(result);
                }
            });
        }
    }

    @Override
    public void onDeleteFinish() {
        if (observer != null) {
            handler.post(() -> {
                if (observer != null) {
                    observer.onDeleteFinish();
                }
            });
        }
    }
}
