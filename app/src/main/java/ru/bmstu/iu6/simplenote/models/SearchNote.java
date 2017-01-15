package ru.bmstu.iu6.simplenote.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;

import java.util.Calendar;

/**
 * Created by Михаил on 10.01.2017.
 */

public class SearchNote implements ISearchNote {
    private final @NonNull INote note;
    private final @NonNull Spanned snippet;

    public SearchNote(@NonNull INote note, @NonNull Spanned snippet) {
        this.note = note;
        this.snippet = snippet;
    }

    @NonNull
    @Override
    public Spanned getSearchSnippet() {
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

    @NonNull
    @Override
    public CharSequence getDescription() {
        return snippet;
    }
}
