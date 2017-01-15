package ru.bmstu.iu6.simplenote.threading;

import android.support.annotation.NonNull;

import rx.Scheduler;

/**
 * Created by Михаил on 15.01.2017.
 */

public interface BaseSchedulerProvider {

    @NonNull
    Scheduler computation();

    @NonNull
    Scheduler io();

    @NonNull
    Scheduler ui();

}
