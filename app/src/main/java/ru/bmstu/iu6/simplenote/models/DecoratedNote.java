package ru.bmstu.iu6.simplenote.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;

/**
 * Created by Михаил on 26.12.2016.
 */

public class DecoratedNote implements INote {
    @NonNull
    private INote note;
    private boolean selected = false;

    public DecoratedNote(@NonNull INote note) {
        this.note = note;
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
        return note.getText();
    }

    @NonNull
    @Override
    public CharSequence getDescription() {
        return note.getDescription();
    }

    @NonNull
    @Override
    public Calendar getDateTime() {
        return note.getDateTime();
    }
}
