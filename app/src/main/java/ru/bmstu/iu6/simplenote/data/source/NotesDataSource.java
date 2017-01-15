package ru.bmstu.iu6.simplenote.data.source;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Set;

import ru.bmstu.iu6.simplenote.models.INote;
import ru.bmstu.iu6.simplenote.models.ISearchNote;
import ru.bmstu.iu6.simplenote.models.Note;
import rx.Observable;

/**
 * Created by Михаил on 27.12.2016.
 */

public interface NotesDataSource {

    @NonNull
    Observable<List<? extends INote>> getNotes();

    @NonNull
    Observable<Long> saveNote(@NonNull INote note);

    @NonNull
    Observable<Void> deleteNotes(@NonNull Set<Integer> nids);

    @NonNull
    Observable<INote> getNote(int nid);

    @NonNull
    Observable<List<? extends ISearchNote>> getNotes(@NonNull String query);

}
