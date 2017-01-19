package ru.bmstu.iu6.simplenote;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

import net.sqlcipher.database.SQLiteDatabase;

/**
 * Created by Михаил on 15.01.2017.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
        SQLiteDatabase.loadLibs(this);
    }
}
