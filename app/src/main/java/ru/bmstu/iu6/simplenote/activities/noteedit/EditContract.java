package ru.bmstu.iu6.simplenote.activities.noteedit;

import android.text.Editable;

import ru.bmstu.iu6.simplenote.mvp.BasePresenter;
import ru.bmstu.iu6.simplenote.mvp.BaseView;

/**
 * Created by Михаил on 27.12.2016.
 */

interface EditContract {
    interface View extends BaseView<Presenter> {

        void setEditMode(boolean enabled);

        void initNoteText(CharSequence text);

        Editable getNoteText();

        void showSaveMessage();
    }

    interface Presenter extends BasePresenter {

        void toggleEditable();

    }
}
