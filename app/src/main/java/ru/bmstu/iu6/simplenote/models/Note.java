package ru.bmstu.iu6.simplenote.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannedString;

import java.util.Calendar;

/**
 * Created by Михаил on 25.12.2016.
 */

public class Note implements INote {
    @Nullable
    private Integer nid;
    @NonNull
    private String text;
    @NonNull
    private Calendar dateTime;

    public Note(@NonNull  String text, @NonNull Calendar dateTime) {
        this.text = text;
        this.dateTime = dateTime;
    }

    public Note(int nid, @NonNull  String text, @NonNull Calendar dateTime) {
        this(text, dateTime);
        this.nid  = nid;
    }

    public void setNid(@Nullable Integer nid) {
        this.nid = nid;
    }

    @Nullable
    @Override
    public Integer getNid() {
        return nid;
    }

    @NonNull
    @Override
    public String getText() {
        return text;
    }

    @NonNull
    @Override
    public Calendar getDateTime() {
        return dateTime;
    }

    @NonNull
    @Override
    public CharSequence getDescription() {
        return text;
    }
}
