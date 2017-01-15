package ru.bmstu.iu6.simplenote.activities.notes;

import java.util.List;

import ru.bmstu.iu6.simplenote.models.DecoratedNote;
import ru.bmstu.iu6.simplenote.mvp.BasePresenter;
import ru.bmstu.iu6.simplenote.mvp.BaseView;

/**
 * Created by Михаил on 27.12.2016.
 */

interface NotesContract {

    interface View extends BaseView<Presenter> {

        void showNotes(List<DecoratedNote> notes);

        void showSelectedCount(int counterValue, Integer position);

        void showNoteDetails(int nid);

        void showCreateNote();
    }

    interface Presenter extends BasePresenter {
        int getDataSetSize();

        void loadNotes(boolean forceUpdate);

        void searchNotes(String searchString);

        void finishSearch();

        void deleteNotes();

        void unSelectNotes();

        void toggleNoteSelection(int position);

        void openNoteDetails(int position);

        void createNewNote();
    }
}
