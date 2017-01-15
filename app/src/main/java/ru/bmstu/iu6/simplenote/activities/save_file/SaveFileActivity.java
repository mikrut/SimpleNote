package ru.bmstu.iu6.simplenote.activities.save_file;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.bmstu.iu6.simplenote.R;
import ru.bmstu.iu6.simplenote.activities.adapters.FilesAdapter;
import ru.bmstu.iu6.simplenote.activities.adapters.IOnItemClickListener;
import ru.bmstu.iu6.simplenote.data.database.NotesDAO;
import ru.bmstu.iu6.simplenote.data.source.NotesDataSource;
import ru.bmstu.iu6.simplenote.data.source.NotesRepository;
import ru.bmstu.iu6.simplenote.data.source.NotesRepositoryService;

public class SaveFileActivity
        extends AppCompatActivity
        implements IOnItemClickListener, SaveFileContract.View {
    public static final String EXTRA_NID =
            SaveFileActivity.class.getCanonicalName() + ".EXTRA_NID";
    public static final String SAVED_FILENAME =
            SaveFileActivity.class.getCanonicalName() + ".SAVED_FILENAME";

    private static final int REQUEST_WRITE_STORAGE = 112;

    private FilesAdapter filesAdapter;
    private EditText fileName;
    private ImageButton saveFile;

    private SaveFileContract.Presenter presenter;
    private int nid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_file);

        Intent incomingIntent = getIntent();
        if (incomingIntent.hasExtra(EXTRA_NID)) {
            nid = incomingIntent.getIntExtra(EXTRA_NID, -1);
        } else {
            finish();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar bar = getSupportActionBar();
        if (bar != null)
            bar.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());

        fileName = (EditText) findViewById(R.id.edit_file_name);
        fileName.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
            String currentContent = dest.toString();
            String result = currentContent.substring(0, dstart) +
                    source.subSequence(start, end) + currentContent.substring(dend);

            final int MAX_FILENAME_LENGTH = 20;

            final String fileNameRegex = "^\\w[\\w,\\s]*\\.?[\\w]*[\\s]*";
            Pattern p = Pattern.compile(fileNameRegex);
            Matcher m = p.matcher(result);
            return ((m.matches() && result.length() <= MAX_FILENAME_LENGTH)
                    || result.length() == 0) ? null : "";
        }});

        saveFile = (ImageButton) findViewById(R.id.button_save_file);
        saveFile.setOnClickListener(view -> {
            NotesDataSource source = NotesDAO.getInstance(getApplicationContext());
            presenter.saveFile(fileName.getText().toString(), source);
        });

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
            new SaveFilePresenter(nid, this, Environment.getExternalStorageDirectory());
            presenter.start();
        }
    }

    @Override
    public void onBackPressed() {
        if (presenter != null)
            presenter.back();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (presenter != null)
            presenter.onDestroyUI();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new SaveFilePresenter(nid, this, Environment.getExternalStorageDirectory());
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
    public void displayFilenameErrorMessage(@NonNull String errorMessage) {
        fileName.setError(errorMessage);
    }

    @Override
    public void stopExecution() {
        finish();
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_FILENAME, fileName.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fileName.setText(savedInstanceState.getString(SAVED_FILENAME));
    }
}
