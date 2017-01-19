package ru.bmstu.iu6.simplenote.activities.notes;

import android.support.annotation.NonNull;
import android.support.v4.internal.view.SupportSubMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ru.bmstu.iu6.simplenote.models.DecoratedNote;
import ru.bmstu.iu6.simplenote.data.source.NotesRepository;
import ru.bmstu.iu6.simplenote.models.INote;
import ru.bmstu.iu6.simplenote.threading.BaseSchedulerProvider;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by Михаил on 27.12.2016.
 */
class NotePresenter implements NotesContract.Presenter {
    @NonNull
    private final NotesContract.View notesView;
    @NonNull
    private final BaseSchedulerProvider schedulerProvider;
    @NonNull
    private final NotesRepository repository;

    @NonNull
    private final CompositeSubscription subscriptions;

    private int selectedCounter = 0;
    private List<DecoratedNote> notes = new ArrayList<>();
    private boolean inSearchState = false;

    private Set<Integer> selectedNotes;

    public NotePresenter(@NonNull NotesContract.View notesView,
                         @NonNull BaseSchedulerProvider schedulerProvider,
                         @NonNull NotesRepository repository) {
        this.notesView = notesView;
        this.schedulerProvider = schedulerProvider;
        this.repository = repository;

        this.subscriptions = new CompositeSubscription();

        notesView.setPresenter(this);
    }

    @Override
    public void start() {
        loadNotesFromDB();
    }

    private void onGetNotes(List<? extends INote> data) {
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
        Subscription subscription = repository.deleteNotes(noteSet)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(nothing -> {
            selectedCounter = 0;
            notesView.showSelectedCount(selectedCounter, null);
        });
        subscriptions.add(subscription);
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

    private void loadNotesFromDB() {
        Subscription subscription = repository.getNotes()
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(this::onGetNotes);
        subscriptions.add(subscription);
    }

    @Override
    public void loadNotes(boolean forceUpdate) {
        if (forceUpdate) {
            // TODO: refresh repository (?)
            loadNotesFromDB();
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
    public void searchNotes(String searchString) {
        inSearchState = true;
        Subscription subscription = repository.getNotes(searchString)
                .subscribeOn(schedulerProvider.computation())
                .observeOn(schedulerProvider.ui())
                .subscribe(searchNotes -> {
            if (inSearchState) {
                if (selectedNotes != null) {
                    selectedCounter = 0;
                    notesView.showSelectedCount(selectedCounter, null);
                    selectedNotes = null;
                }

                onGetNotes(searchNotes);
            }
        });
        subscriptions.add(subscription);
    }

    @Override
    public void unsubscribe() {
        subscriptions.clear();
    }

    @Override
    public void finishSearch() {
        inSearchState = false;
        repository.getNotes();
    }
}
