package ru.bmstu.iu6.simplenote.models;

import android.support.annotation.NonNull;

/**
 * Created by Михаил on 10.01.2017.
 */

public interface ISearchNote extends INote {
    @NonNull
    String getSearchSnippet();
}
