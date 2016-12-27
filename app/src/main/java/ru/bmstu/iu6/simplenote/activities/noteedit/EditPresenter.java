package ru.bmstu.iu6.simplenote.activities.noteedit;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.GregorianCalendar;
import java.util.List;

import ru.bmstu.iu6.simplenote.data.source.NotesRepository;
import ru.bmstu.iu6.simplenote.data.source.NotesRepositoryService;
import ru.bmstu.iu6.simplenote.models.INote;
import ru.bmstu.iu6.simplenote.models.Note;

/**
 * Created by Михаил on 27.12.2016.
 */
class EditPresenter implements EditContract.Presenter, NotesRepositoryService.NotesRepositoryObserver {
    private EditContract.View view;
    private NotesRepository repository;
    private Integer nid;

    private boolean editable;

    EditPresenter(@NonNull EditContract.View editView, @NonNull Handler handler,
                  @NonNull NotesRepository repository, @Nullable Integer nid,
                  boolean editable) {
        this.repository = repository;
        repository.setContentObserver(this, handler);
        this.view = editView;
        editView.setPresenter(this);
        this.nid = nid;
        this.editable = editable;
        view.setEditMode(editable);
    }

    void notifyServiceDisconnected() {
        repository.removeContentObserver();
        repository = null;
    }

    @Override
    public void start() {
        if (nid != null) {
            repository.getNote(nid);
        }
    }

    @Override
    public void toggleEditable() {
        if (editable) {
            saveNote();
        }
        view.setEditMode(editable = !editable);
    }

    private void saveNote() {
        Note note = new Note(view.getNoteText().toString(), GregorianCalendar.getInstance());
        note.setNid(nid);
        if (note.getText().length() > 0 || note.getNid() != null) {
            // TODO: fix NIDs to long (?)
            repository.saveNote(note);
        }
    }

    @Override
    public void onGetNotes(List<? extends INote> notes) {
        // Nothing
    }

    @Override
    public void onGetNote(INote note) {
        if (note != null)
            view.initNoteText(note.getText());
    }

    @Override
    public void onSaveNoteResult(long result) {
        if (nid == null)
            nid = result != -1 ? (int) result : null;
    }

    @Override
    public void onDeleteFinish() {
        // nothinig
        // TODO: consider deletion feature
    }

    void onFinish() {
        if (editable) {
            saveNote();
        }
    }
}
