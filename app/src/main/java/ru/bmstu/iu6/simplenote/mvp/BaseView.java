package ru.bmstu.iu6.simplenote.mvp;

import android.support.annotation.NonNull;

/**
 * Created by Михаил on 27.12.2016.
 */

public interface BaseView<Presenter> {
    void setPresenter(@NonNull Presenter presenter);
}
