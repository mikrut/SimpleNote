package ru.bmstu.iu6.simplenote.activities.save_file;

import java.io.File;
import java.util.List;

import ru.bmstu.iu6.simplenote.mvp.BasePresenter;
import ru.bmstu.iu6.simplenote.mvp.BaseView;

/**
 * Created by Михаил on 04.01.2017.
 */

public interface SaveFileContract {
    interface View extends BaseView<Presenter> {
        void displayFilesList(List<? extends File> files);
    }

    interface Presenter extends BasePresenter {
        void onPositionClick(int position);
    }
}
