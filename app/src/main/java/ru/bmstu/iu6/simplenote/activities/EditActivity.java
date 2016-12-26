package ru.bmstu.iu6.simplenote.activities;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Calendar;
import java.util.GregorianCalendar;

import ru.bmstu.iu6.simplenote.R;
import ru.bmstu.iu6.simplenote.database.NotesDAO;
import ru.bmstu.iu6.simplenote.models.Note;

import static ru.bmstu.iu6.simplenote.R.styleable.AppBarLayout;

public class EditActivity extends AppCompatActivity {
    public static final String EXTRA_EDITABLE =
            EditActivity.class.getCanonicalName() + ".EXTRA_EDITABLE";
    public static final String EXTRA_NID =
            EditActivity.class.getCanonicalName() + ".EXTRA_NID";

    private EditText noteEdit;
    private Toolbar toolbar;
    private AppBarLayout appBar;

    private final boolean DEFAULT_EDITABLE = false;
    private boolean editable;
    private Integer nid;
    private NotesDAO notesDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        noteEdit = (EditText) findViewById(R.id.edit_note);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        appBar = (AppBarLayout) findViewById(R.id.app_bar);

        noteEdit.setFilters(new InputFilter[] {
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) {
                        return editable ? null : "";
                    }
                }
        });
        noteEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (editable) {
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

        notesDAO = new NotesDAO(this);

        Intent intent = getIntent();
        nid = intent.hasExtra(EXTRA_NID) ? intent.getIntExtra(EXTRA_NID, -1) : null;
        setEditable(intent.getBooleanExtra(EXTRA_EDITABLE, DEFAULT_EDITABLE));
        if (editable && nid == null) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        notesDAO.open();
        if (nid != null) {
            Note note = notesDAO.getNote(nid);
            editable = true;
            if (note != null)
                noteEdit.setText(note.getText());
            editable = false;
        }
    }

    @Override
    protected void onPause() {
        if (editable) {
            saveNote();
        }
        notesDAO.close();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
            case R.id.action_edit:
                if (editable) {
                    saveNote();
                }
                setEditable(!editable); // toggle editable state
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        @DrawableRes int resid =
                editable ? R.drawable.ic_done_white_24dp : R.drawable.ic_mode_edit_white_24dp;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            menu.getItem(0).setIcon(getResources().getDrawable(resid, getTheme()));
        } else {
            menu.getItem(0).setIcon(getResources().getDrawable(resid));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void saveNote() {
        Note note = new Note(noteEdit.getText().toString(), GregorianCalendar.getInstance());
        note.setNid(nid);
        if (note.getText().length() > 0 || note.getNid() != null) {
            // TODO: fix NIDs to long (?)
            long inserted_id = notesDAO.saveNote(note);
            if (nid == null)
                nid = inserted_id != -1 ? (int) inserted_id : null;
            // TODO: use string resources
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void setEditable(boolean editable) {
        this.editable = editable;

        InputMethodManager imm = (InputMethodManager)
                EditActivity.this.getSystemService(Service.INPUT_METHOD_SERVICE);
        if (editable) {
            noteEdit.requestFocus();
            imm.showSoftInput(noteEdit, 0);
        } else {
            imm.hideSoftInputFromWindow(noteEdit.getWindowToken(), 0);
        }

        invalidateOptionsMenu();
    }
}

