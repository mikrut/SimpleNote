package ru.bmstu.iu6.simplenote.activities.adapters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;

import ru.bmstu.iu6.simplenote.models.INote;
import ru.bmstu.iu6.simplenote.models.ISearchNote;
import ru.bmstu.iu6.simplenote.models.SearchNote;

/**
 * Created by Михаил on 26.12.2016.
 */

public class DecoratedNote implements INote {
    @NonNull
    private INote note;
    private boolean selected = false;
    private final boolean isSearchNote; // TODO: consider some other architectural approach

    public DecoratedNote(@NonNull INote note) {
        this.note = note;
        isSearchNote = (note instanceof ISearchNote);
    }

    public boolean isSearchNote() {
        return isSearchNote;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Nullable
    @Override
    public Integer getNid() {
        return note.getNid();
    }

    @NonNull
    @Override
    public String getText() {
        // FIXME: seems to be a bad idea...
        if (isSearchNote) {
            return ((ISearchNote) note).getSearchSnippet();
        } else {
            return note.getText();
        }
    }

    @NonNull
    @Override
    public Calendar getDateTime() {
        return note.getDateTime();
    }
}
