package ru.bmstu.iu6.simplenote.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Calendar;

/**
 * Created by Михаил on 10.01.2017.
 */

public class SearchNote implements ISearchNote {
    private final @NonNull INote note;
    private final @NonNull String snippet;

    public SearchNote(@NonNull INote note, @NonNull String snippet) {
        this.note = note;
        this.snippet = snippet;
    }

    @NonNull
    @Override
    public String getSearchSnippet() {
        return snippet;
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
    public Calendar getDateTime() {
        return note.getDateTime();
    }
}
