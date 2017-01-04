package ru.bmstu.iu6.simplenote.models;

import android.support.annotation.NonNull;

import java.io.File;
import java.net.URI;

/**
 * Created by Михаил on 04.01.2017.
 */

public class ArbitrarilyNamedFile extends File {
    private String customName;

    public ArbitrarilyNamedFile(String customName, String pathname) {
        super(pathname);
        this.customName = customName;
    }

    public ArbitrarilyNamedFile(String customName, URI uri) {
        super(uri);
        this.customName = customName;
    }

    @NonNull
    @Override
    public String getName() {
        return (customName != null) ? customName : super.getName();
    }
}
