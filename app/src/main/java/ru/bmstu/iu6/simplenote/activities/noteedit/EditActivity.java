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
import ru.bmstu.iu6.simplenote.activities.login.LoginActivity;
import ru.bmstu.iu6.simplenote.activities.save_file.SaveFileActivity;
import ru.bmstu.iu6.simplenote.data.database.NotesDAO;
import ru.bmstu.iu6.simplenote.data.source.NotesRepository;
import ru.bmstu.iu6.simplenote.threading.BaseSchedulerProvider;
import ru.bmstu.iu6.simplenote.threading.SchedulerProvider;

public class EditActivity extends AppCompatActivity {
    public static final String EXTRA_EDITABLE =
            EditActivity.class.getCanonicalName() + ".EXTRA_EDITABLE";
    public static final String EXTRA_NID =
            EditActivity.class.getCanonicalName() + ".EXTRA_NID";

    public static final String SAVED_NID =
            EditActivity.class.getCanonicalName() + ".SAVED_NID";

    private static final boolean DEFAULT_EDITABLE = false;

    private EditPresenter presenter;

    private Integer nid;
    private boolean editable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        view.onCreate(savedInstanceState);

        NotesDAO localSource = NotesDAO.getInstance(getApplicationContext());
        if (localSource == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        NotesRepository repository = NotesRepository.getInstance(localSource);
        BaseSchedulerProvider schedulerProvider = SchedulerProvider.getInstance();
        presenter = new EditPresenter(view, schedulerProvider,
                repository, nid, editable);
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.start();
    }

    @Override
    protected void onPause() {
        presenter.unsubscribe();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (nid != null)
            outState.putInt(SAVED_NID, nid);
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
                    (charSequence, i, i1, spanned, i2, i3) -> inEditMode ? null : ""
            });
            noteEdit.setOnClickListener(view1 -> {
                if (inEditMode) {
                    InputMethodManager imm = (InputMethodManager)
                            EditActivity.this.getSystemService(Service.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(view1, 0);
                }
            });

            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(view12 -> onBackPressed());

            Intent intent = getIntent();
            nid = intent.hasExtra(EXTRA_NID) ? intent.getIntExtra(EXTRA_NID, -1) : null;
            editable = intent.getBooleanExtra(EXTRA_EDITABLE, DEFAULT_EDITABLE);

            if (savedInstanceState != null && savedInstanceState.containsKey(SAVED_NID))
                nid = savedInstanceState.getInt(SAVED_NID);

            view.enableCreationMode(editable && nid == null);
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
                // Inside version check
                //noinspection deprecation
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
        public void rememberNid(int nid) {
            EditActivity.this.nid = nid;
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
        public void showCantSaveEmpty() {
            // TODO: use string resources
            Toast.makeText(context, "Cant save empty note", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void showSaveActivity() {
            Intent saveIntent = new Intent(EditActivity.this, SaveFileActivity.class);
            saveIntent.putExtra(SaveFileActivity.EXTRA_NID, nid);
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

