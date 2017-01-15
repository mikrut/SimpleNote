package ru.bmstu.iu6.simplenote.activities.noteedit;

import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.GregorianCalendar;
import java.util.List;

import ru.bmstu.iu6.simplenote.data.source.NotesRepository;
import ru.bmstu.iu6.simplenote.models.INote;
import ru.bmstu.iu6.simplenote.models.ISearchNote;
import ru.bmstu.iu6.simplenote.models.Note;
import ru.bmstu.iu6.simplenote.threading.BaseSchedulerProvider;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by Михаил on 27.12.2016.
 */
class EditPresenter implements EditContract.Presenter {
    @NonNull
    private final EditContract.View view;
    @NonNull
    private final NotesRepository repository;
    @NonNull
    private final BaseSchedulerProvider schedulerProvider;

    @NonNull
    private final CompositeSubscription subscriptions;

    private Integer nid;
    private boolean editable;
    private boolean shouldOpenSaveActivity = false;

    EditPresenter(@NonNull EditContract.View editView, @NonNull BaseSchedulerProvider schedulerProvider,
                  @NonNull NotesRepository repository, @Nullable Integer nid,
                  boolean editable) {
        this.repository = repository;
        this.schedulerProvider = schedulerProvider;
        this.view = editView;

        editView.setPresenter(this);
        this.subscriptions = new CompositeSubscription();
        this.nid = nid;
        this.editable = editable;
        view.setEditMode(editable);
    }

    @Override
    public void start() {
        if (nid != null) {
            Subscription subscription = repository.getNote(nid)
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
                    .subscribe(note -> {
                if (note != null)
                    view.initNoteText(note.getText());
            });
            subscriptions.add(subscription);
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
            Subscription subscription = repository.saveNote(note)
                    .subscribeOn(schedulerProvider.computation())
                    .observeOn(schedulerProvider.ui())
                    .subscribe(result -> {
                if (nid == null) {
                    nid = result != -1 ? result.intValue() : null;
                    view.showSaveMessage();
                    view.rememberNid(nid);

                    if (shouldOpenSaveActivity)
                        view.showSaveActivity();
                }
                shouldOpenSaveActivity = false;
            });
            subscriptions.add(subscription);
        }
    }

    @Override
    public void share() {
        String text = view.getNoteText().toString();
        if (!text.equals("")) {
            view.startShareActivity(text);
        }
    }

    @Override
    public void saveAsTxt() {
        if (nid != null) {
            view.showSaveActivity();
        } else if (view.getNoteText().length() > 0) {
            shouldOpenSaveActivity = true;
            saveNote();
        } else {
            view.showCantSaveEmpty();
        }
    }

    @Override
    public void unsubscribe() {
        subscriptions.unsubscribe();
        if (editable) {
            saveNote();
        }
    }
}
