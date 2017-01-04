package ru.bmstu.iu6.simplenote.activities.save_file;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.io.File;
import java.util.List;

import ru.bmstu.iu6.simplenote.R;
import ru.bmstu.iu6.simplenote.activities.adapters.FilesAdapter;
import ru.bmstu.iu6.simplenote.activities.adapters.IOnItemClickListener;

public class SaveFileActivity
        extends AppCompatActivity
        implements IOnItemClickListener, SaveFileContract.View {
    private static final int REQUEST_WRITE_STORAGE = 112;

    private FilesAdapter filesAdapter;

    private SaveFileContract.Presenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_file);

        RecyclerView filesRecycler = (RecyclerView) findViewById(R.id.recycler_files);
        filesRecycler.setLayoutManager(new LinearLayoutManager(this));
        filesAdapter = new FilesAdapter(this);
        filesAdapter.setOnItemClickListener(this);
        filesRecycler.setAdapter(filesAdapter);

        boolean hasPermission = true;
        boolean hasWPermission = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            final int readPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
            final int writePermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            hasPermission = (readPermission == PackageManager.PERMISSION_GRANTED);
            hasWPermission = (writePermission == PackageManager.PERMISSION_GRANTED);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                (!hasPermission || !hasWPermission)) {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    REQUEST_WRITE_STORAGE);
        } else {
            new SaveFilePresenter(this, Environment.getExternalStorageDirectory());
            presenter.start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new SaveFilePresenter(this, Environment.getExternalStorageDirectory());
                    presenter.start();
                } else {
                    finish();
                }
            }
        }
    }

    @Override
    public void displayFilesList(List<? extends File> files) {
        filesAdapter.replaceFiles(files);
    }

    @Override
    public void setPresenter(@NonNull SaveFileContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onLongClick(int position) {
        // nothing for now
    }

    @Override
    public void onClick(int position) {
        if (presenter != null)
            presenter.onPositionClick(position);
    }
}
