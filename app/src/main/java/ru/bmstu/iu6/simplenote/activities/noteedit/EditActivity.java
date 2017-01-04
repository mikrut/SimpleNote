package ru.bmstu.iu6.simplenote.activities.noteedit;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import ru.bmstu.iu6.simplenote.R;
import ru.bmstu.iu6.simplenote.activities.save_file.SaveFileActivity;
import ru.bmstu.iu6.simplenote.data.source.NotesRepository;
import ru.bmstu.iu6.simplenote.data.source.NotesRepositoryService;

public class EditActivity extends AppCompatActivity {
    public static final String EXTRA_EDITABLE =
            EditActivity.class.getCanonicalName() + ".EXTRA_EDITABLE";
    public static final String EXTRA_NID =
            EditActivity.class.getCanonicalName() + ".EXTRA_NID";

    private static final boolean DEFAULT_EDITABLE = false;

    private Integer nid;
    private boolean editable;

    private EditPresenter presenter;

    private boolean bound = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            NotesRepository repository = (NotesRepository) iBinder;
            presenter = new EditPresenter(view, new Handler(Looper.getMainLooper()),
                    repository, nid, editable);
            presenter.start();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;
            presenter.notifyServiceDisconnected();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        Intent intent = getIntent();
        nid = intent.hasExtra(EXTRA_NID) ? intent.getIntExtra(EXTRA_NID, -1) : null;
        editable = intent.getBooleanExtra(EXTRA_EDITABLE, DEFAULT_EDITABLE);

        view.onCreate(savedInstanceState);
        view.enableCreationMode(editable && nid == null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, NotesRepositoryService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(mConnection);
            bound = false;
        }
    }

    @Override
    protected void onPause() {
        presenter.onFinish();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return view.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return view.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return view.onPrepareOptionsMenu(menu);
    }

    private EditView view = new EditView();

    class EditView implements EditContract.View {
        private EditContract.Presenter presenter;

        private EditText noteEdit;
        private Toolbar toolbar;
        private AppBarLayout appBar;

        private boolean inEditMode = false;
        private Context context = EditActivity.this;

        public void onCreate(@Nullable Bundle savedInstanceState) {
            noteEdit = (EditText) findViewById(R.id.edit_note);
            toolbar = (Toolbar) findViewById(R.id.toolbar);
            appBar = (AppBarLayout) findViewById(R.id.app_bar);

            noteEdit.setFilters(new InputFilter[] {
                    new InputFilter() {
                        @Override
                        public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) {
                            return inEditMode ? null : "";
                        }
                    }
            });
            noteEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (inEditMode) {
                        InputMethodManager imm = (InputMethodManager)
                                EditActivity.this.getSystemService(Service.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(view, 0);
                    }
                }
            });

            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onBackPressed();
                }
            });
        }

        public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.menu_edit, menu);
            return true;
        }

        public boolean onPrepareOptionsMenu(Menu menu) {
            @DrawableRes int resid =
                    inEditMode ? R.drawable.ic_done_white_24dp : R.drawable.ic_mode_edit_white_24dp;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                menu.getItem(0).setIcon(getResources().getDrawable(resid, getTheme()));
            } else {
                menu.getItem(0).setIcon(getResources().getDrawable(resid));
            }
            return true;
        }

        public boolean onOptionsItemSelected(MenuItem item) {
            final int id = item.getItemId();
            switch (id) {
                case R.id.action_edit:
                    presenter.toggleEditable();
                    return true;
                case R.id.action_share:
                    presenter.share();
                    return true;
                case R.id.action_export_txt:
                    presenter.saveAsTxt();
                    return true;
                default:
                    return true;
            }
        }

        public void enableCreationMode(boolean enable){
            getWindow().setSoftInputMode(enable ?
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED :
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
            );
        }

        @Override
        public void initNoteText(CharSequence text) {
            inEditMode = true;
            noteEdit.setText(text);
            inEditMode = false;
        }

        @Override
        public void setEditMode(boolean enabled) {
            inEditMode = enabled;

            InputMethodManager imm = (InputMethodManager)
                    EditActivity.this.getSystemService(Service.INPUT_METHOD_SERVICE);
            if (inEditMode) {
                noteEdit.requestFocus();
                imm.showSoftInput(noteEdit, 0);
            } else {
                imm.hideSoftInputFromWindow(noteEdit.getWindowToken(), 0);
            }

            invalidateOptionsMenu();
        }

        @Override
        public void setPresenter(@NonNull EditContract.Presenter presenter) {
            this.presenter = presenter;
        }

        @Override
        public Editable getNoteText() {
            return noteEdit.getText();
        }

        @Override
        public void showSaveMessage() {
            // TODO: use string resources
            Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void showSaveActivity() {
            Intent saveIntent = new Intent(EditActivity.this, SaveFileActivity.class);
            startActivity(saveIntent);
        }

        @Override
        public void startShareActivity(String text) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            shareIntent.setType("text/plain");
            // TODO: use string resources
            Intent chooser = Intent.createChooser(shareIntent, "Share note to...");
            startActivity(chooser);
        }
    }

}

