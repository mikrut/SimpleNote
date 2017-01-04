package ru.bmstu.iu6.simplenote.activities.save_file;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.bmstu.iu6.simplenote.models.ArbitrarilyNamedFile;

/**
 * Created by Михаил on 04.01.2017.
 */

class SaveFilePresenter implements SaveFileContract.Presenter {
    private static final String ROOT = "/";
    private static final String UP = "../";

    private final SaveFileContract.View mView;

    private final String rootDir;
    private File currentDir;
    private ArrayList<File> files;

    SaveFilePresenter(@NonNull SaveFileContract.View view, @NonNull File startDir) {
        mView = view;
        view.setPresenter(this);
        String startPath = ROOT;
        try {
            startPath = (currentDir = startDir).getCanonicalPath();
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }
        rootDir = startPath;
    }

    @Override
    public void onPositionClick(int position) {
        File chosenFile = files.get(position);
        if (chosenFile.isDirectory()) {
            currentDir = chosenFile;
            updateFilesList();
        }
    }

    private void updateFilesList() {
        List<File> dirFiles = Arrays.asList(currentDir.listFiles());
        int capacity = dirFiles.size() + 2;
        if (files == null) {
            files = new ArrayList<>(capacity);
        } else {
            files.ensureCapacity(capacity);
        }

        try {
            if (!currentDir.getCanonicalPath().equals(rootDir)) {
                files.add(new ArbitrarilyNamedFile(ROOT, rootDir));
                files.add(new ArbitrarilyNamedFile(UP, currentDir.getParent()));
            }
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }

        for (File f : dirFiles) {
            if (!f.isHidden())
                files.add(f);
        }
        Collections.sort(files);

        mView.displayFilesList(files);
    }

    @Override
    public void start() {
        updateFilesList();
    }
}
