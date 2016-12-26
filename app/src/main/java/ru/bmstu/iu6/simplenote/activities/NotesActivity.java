package ru.bmstu.iu6.simplenote.activities;

import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ru.bmstu.iu6.simplenote.R;
import ru.bmstu.iu6.simplenote.adapters.DecoratedNote;
import ru.bmstu.iu6.simplenote.adapters.NotesAdapter;
import ru.bmstu.iu6.simplenote.database.NotesDAO;
import ru.bmstu.iu6.simplenote.loaders.NotesLoader;
import ru.bmstu.iu6.simplenote.models.INote;
import ru.bmstu.iu6.simplenote.models.Note;

public class NotesActivity
        extends AppCompatActivity
        implements NotesAdapter.IOnItemClickListener{
    private static final String SAVED_SELECTED =
            NotesActivity.class.getCanonicalName() + ".SAVED_SELECTED";

    private RecyclerView notesRecycler;
    private NotesAdapter adapter;
    private TextView emptyPlaceholder;

    private List<DecoratedNote> notes;
    private int selectedCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NotesActivity.this, EditActivity.class);
                intent.putExtra(EditActivity.EXTRA_EDITABLE, true);
                startActivity(intent);
            }
        });

        notesRecycler = (RecyclerView) findViewById(R.id.recycler_notes);

        LinearLayoutManager liman = new LinearLayoutManager(this);
        notesRecycler.setLayoutManager(liman);

        notes = new ArrayList<>();
        adapter = new NotesAdapter(this, notes);
        adapter.setOnItemClickListener(this);
        notesRecycler.setAdapter(adapter);

        emptyPlaceholder = (TextView) findViewById(R.id.text_empty_placeholder);

        getLoaderManager().initLoader(NotesLoaderCallbacks.LOADER_NOTES, null, callbacks);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(NotesLoaderCallbacks.LOADER_NOTES, null, callbacks);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_notes, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        NotesAdapter adapter = (NotesAdapter) notesRecycler.getAdapter();
        menu.clear();
        if (selectedCounter > 0) {
            getMenuInflater().inflate(R.menu.menu_notes_selected, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_notes, menu);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onLongClick(int position) {
        if (selectedCounter == 0) {
            DecoratedNote note = notes.get(position);
            note.setSelected(true);
            selectedCounter = 1;
            onCounterUpdate();
            adapter.notifyItemChanged(position);
        }
    }

    @Override
    public void onClick(int position) {
        DecoratedNote note = notes.get(position);
        if (note != null) {
            if (selectedCounter > 0) {
                boolean nextState = !note.isSelected();
                selectedCounter += nextState ? 1 : -1;
                onCounterUpdate();
                note.setSelected(nextState);
                adapter.notifyItemChanged(position);
            } else {
                if (note.getNid() != null) {
                    Intent intent = new Intent(this, EditActivity.class);
                    intent.putExtra(EditActivity.EXTRA_EDITABLE, false);
                    intent.putExtra(EditActivity.EXTRA_NID, note.getNid());
                    startActivity(intent);
                }
            }
        }
    }

    public void clearSelections() {
        for (DecoratedNote note : notes) {
            note.setSelected(false);
        }
        adapter.notifyDataSetChanged();
        selectedCounter = 0;
        onCounterUpdate();
    }

    public void onCounterUpdate() {
        final int counterValue = selectedCounter;
        if (counterValue > 0) {
            // TODO: use string resource
            setTitle("Выбрано: " + counterValue);
            if (counterValue == 1)
                invalidateOptionsMenu();
        } else {
            String label = "Notes";
            try {
                PackageManager pm = getPackageManager();
                ComponentName componentName = getComponentName();
                ActivityInfo activityInfo = pm.getActivityInfo(componentName, 0);
                label = activityInfo.loadLabel(pm).toString();
            } catch (PackageManager.NameNotFoundException ex) {
                Log.e(NotesActivity.class.getSimpleName(), ex.getLocalizedMessage());
            }
            setTitle(label);
            invalidateOptionsMenu();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        NotesAdapter adapter = (NotesAdapter) notesRecycler.getAdapter();

        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_clear:
                clearSelections();
                return true;
            case R.id.action_delete:
                Set<Integer> noteSet = filterSelected(true);
                // TODO: move to some async task (?)
                NotesDAO dao = new NotesDAO(this);
                dao.open();
                dao.deleteNotes(noteSet);
                dao.close();
                selectedCounter = 0;
                onCounterUpdate();

                adapter.notifyDataSetChanged();
                if (notes.size() == 0) {
                    emptyPlaceholder.setVisibility(View.VISIBLE);
                    notesRecycler.setVisibility(View.GONE);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Set<Integer> filterSelected() {
        return filterSelected(false);
    }

    private Set<Integer> filterSelected(boolean exclude) {
        Set<Integer> noteSet = new TreeSet<>();
        int i = 0;
        while (i < notes.size()) {
            DecoratedNote note = notes.get(i);
            if (note.isSelected()) {
                noteSet.add(note.getNid());
                if (exclude) {
                    notes.remove(i);
                } else {
                    i++;
                }
            } else {
                i++;
            }
        }
        return noteSet;
    }

    private NotesLoaderCallbacks callbacks = new NotesLoaderCallbacks();
    private class NotesLoaderCallbacks implements LoaderManager.LoaderCallbacks<List<Note>> {
        static final int LOADER_NOTES = 0;

        @Override
        public Loader<List<Note>> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_NOTES:
                    return new NotesLoader(NotesActivity.this);
                default:
                    return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<List<Note>> loader, List<Note> data) {
            NotesAdapter adapter = (NotesAdapter) notesRecycler.getAdapter();
            notes.clear();
            for (Note note : data) {
                DecoratedNote decoratedNote = new DecoratedNote(note);
                if (decoratedNotes != null && decoratedNotes.contains(decoratedNote.getNid()))
                    decoratedNote.setSelected(true);
                notes.add(decoratedNote);
            }
            if (decoratedNotes != null) {
                selectedCounter = decoratedNotes.size();
                onCounterUpdate();
            }
            decoratedNotes = null;
            adapter.notifyDataSetChanged();
            if (data.size() > 0) {
                emptyPlaceholder.setVisibility(View.GONE);
                notesRecycler.setVisibility(View.VISIBLE);
            } else {
                emptyPlaceholder.setVisibility(View.VISIBLE);
                notesRecycler.setVisibility(View.GONE);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<Note>> loader) {
            // Nothing
        }
    }

    private Set<Integer> decoratedNotes;

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        decoratedNotes = (Set<Integer>) savedInstanceState.getSerializable(SAVED_SELECTED);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Set<Integer> decoratedNotes = filterSelected();
        outState.putSerializable(SAVED_SELECTED, (Serializable) decoratedNotes);
    }
}
