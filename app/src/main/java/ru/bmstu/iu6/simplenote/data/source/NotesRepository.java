package ru.bmstu.iu6.simplenote.data.source;

import android.os.Binder;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.List;
import java.util.Set;

import ru.bmstu.iu6.simplenote.data.database.NotesDAO;
import ru.bmstu.iu6.simplenote.models.INote;
import ru.bmstu.iu6.simplenote.models.ISearchNote;
import rx.Observable;

/**
 * Created by Михаил on 27.12.2016.
 */

public class NotesRepository implements NotesDataSource {
    @NonNull
    private final NotesDataSource localSource;
    @Nullable
    private static NotesRepository INSTANCE = null;

    // TODO: caching stuff
    private NotesRepository(@NonNull NotesDataSource localSource) {
        this.localSource = localSource;
    }

    public static NotesRepository getInstance(@NonNull NotesDataSource localSource) {
        if (INSTANCE == null) {
            INSTANCE = new NotesRepository(localSource);
        }
        return INSTANCE;
    }

    @NonNull
    @Override
    public Observable<List<? extends INote>> getNotes() {
        return localSource.getNotes();
    }

    @NonNull
    @Override
    public Observable<Long> saveNote(@NonNull INote note) {
        return localSource.saveNote(note);
    }

    @NonNull
    @Override
    public Observable<Void> deleteNotes(@NonNull Set<Integer> nids) {
        return localSource.deleteNotes(nids);
    }

    @NonNull
    @Override
    public Observable<INote> getNote(int nid) {
        return localSource.getNote(nid);
    }

    @NonNull
    @Override
    public Observable<List<? extends ISearchNote>> getNotes(@NonNull String query) {
        return localSource.getNotes(query);
    }
}
