package ru.bmstu.iu6.simplenote.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spanned;

import java.io.Serializable;
import java.util.Calendar;

/**
 * Created by Михаил on 26.12.2016.
 */

public interface INote extends Serializable {
    @Nullable
    Integer getNid();

    @NonNull
    String getText();

    @NonNull
    CharSequence getDescription();

    @NonNull
    Calendar getDateTime();
}
