package ru.bmstu.iu6.simplenote.data.source;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Set;

import ru.bmstu.iu6.simplenote.models.INote;
import ru.bmstu.iu6.simplenote.models.ISearchNote;
import ru.bmstu.iu6.simplenote.models.Note;

/**
 * Created by Михаил on 27.12.2016.
 */

public interface NotesDataSource {

    @Nullable
    List<? extends INote> getNotes();

    long saveNote(@NonNull INote note);

    void deleteNotes(@NonNull Set<Integer> nids);

    @Nullable
    INote getNote(int nid);

    @NonNull
    List<? extends ISearchNote> getNotes(@NonNull String query);

}
