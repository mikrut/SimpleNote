package ru.bmstu.iu6.simplenote.activities.notes;

import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ru.bmstu.iu6.simplenote.activities.adapters.DecoratedNote;
import ru.bmstu.iu6.simplenote.data.source.NotesRepository;
import ru.bmstu.iu6.simplenote.data.source.NotesRepositoryService;
import ru.bmstu.iu6.simplenote.models.INote;

/**
 * Created by Михаил on 27.12.2016.
 */
class NotePresenter
        implements NotesContract.Presenter,
        NotesRepositoryService.NotesRepositoryObserver {
    private NotesContract.View notesView;

    private NotesRepository repository;
    static final int LOADER_NOTES = 0;

    private int selectedCounter = 0;
    private List<DecoratedNote> notes = new ArrayList<>();

    private Set<Integer> selectedNotes;

    public NotePresenter(@NonNull NotesContract.View notesView,
                         @NonNull Handler handler,
                         @NonNull NotesRepository repository) {
        this.notesView = notesView;
        this.repository = repository;
        repository.setContentObserver(this, handler);
        notesView.setPresenter(this);
    }

    @Override
    public void start() {
        repository.getNotes();
    }

    public void notifyServiceDisconnected() {
        repository.removeContentObserver();
        repository = null;
    }

    @Override
    public void openNoteDetails(int position) {
        final INote note = notes.get(position);
        if (note.getNid() != null) {
            notesView.showNoteDetails(note.getNid());
        }
    }

    @Override
    public void toggleNoteSelection(int position) {
        DecoratedNote note = notes.get(position);
        boolean nextState = !note.isSelected();
        selectedCounter += nextState ? 1 : -1;
        note.setSelected(nextState);
        notesView.showSelectedCount(selectedCounter, position);
    }

    @Override
    public void unSelectNotes() {
        for (DecoratedNote note : notes) {
            note.setSelected(false);
        }
        selectedCounter = 0;
        notesView.showSelectedCount(selectedCounter, null);
    }

    @Override
    public void deleteNotes() {
        final Set<Integer> noteSet = filterSelected(true);
        repository.deleteNotes(noteSet);
    }

    @Override
    public void createNewNote() {
        notesView.showCreateNote();
    }

    @Override
    public int getDataSetSize() {
        return notes.size();
    }

    public void restoreSelected(Set<Integer> selected) {
        selectedNotes = selected;
    }

    @Override
    public void loadNotes(boolean forceUpdate) {
        if (forceUpdate) {
            // TODO: refresh repository (?)
        } else {
            notesView.showNotes(notes);
        }
    }

    public Set<Integer> filterSelected(boolean exclude) {
        Set<Integer> noteSet = new TreeSet<>();
        int i = 0;
        while (i < notes.size()) {
            DecoratedNote note = notes.get(i);
            if (note.isSelected()) {
                noteSet.add(note.getNid());
                if (exclude) {
                    notes.remove(i);
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }
        return noteSet;
    }

    @Override
    public void onGetNotes(List<? extends INote> data) {
        notes.clear();

        for (INote note : data) {
            DecoratedNote decoratedNote = new DecoratedNote(note);
            if (selectedNotes != null && selectedNotes.contains(decoratedNote.getNid()))
                decoratedNote.setSelected(true);
            notes.add(decoratedNote);
        }

        notesView.showNotes(notes);

        if (selectedNotes != null) {
            selectedCounter = selectedNotes.size();
            notesView.showSelectedCount(selectedCounter, null);
            selectedNotes = null;
        } else {
            selectedCounter = 0;
            notesView.showSelectedCount(selectedCounter, null);
        }
    }

    @Override
    public void onGetNote(INote note) {

    }

    @Override
    public void onSaveNoteResult(long result) {

    }

    @Override
    public void onDeleteFinish() {
        selectedCounter = 0;
        notesView.showSelectedCount(selectedCounter, null);
    }
}
