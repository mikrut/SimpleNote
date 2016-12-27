package ru.bmstu.iu6.simplenote.activities.notes;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import ru.bmstu.iu6.simplenote.R;
import ru.bmstu.iu6.simplenote.activities.noteedit.EditActivity;
import ru.bmstu.iu6.simplenote.activities.adapters.DecoratedNote;
import ru.bmstu.iu6.simplenote.activities.adapters.NotesAdapter;
import ru.bmstu.iu6.simplenote.data.database.NotesDAO;
import ru.bmstu.iu6.simplenote.data.source.NotesRepository;
import ru.bmstu.iu6.simplenote.data.source.NotesRepositoryService;

public class NotesActivity
        extends AppCompatActivity {
    private static final String SAVED_SELECTED =
            NotesActivity.class.getCanonicalName() + ".SAVED_SELECTED";

    private NotesView view = new NotesView();
    private NotePresenter presenter;

    private Set<Integer> notes;

    private boolean bound = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            NotesRepository repository = (NotesRepository) iBinder;
            presenter = new NotePresenter(view, new Handler(Looper.getMainLooper()), repository);
            presenter.start();
            if (notes != null) {
                presenter.restoreSelected(notes);
                notes = null;
            }
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
        view.onCreate(savedInstanceState);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        return view.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return view.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return view.onOptionsItemSelected(item);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        notes = (Set<Integer>) savedInstanceState.getSerializable(SAVED_SELECTED);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Set<Integer> decoratedNotes = presenter.filterSelected(false);
        outState.putSerializable(SAVED_SELECTED, (Serializable) decoratedNotes);
    }

    private class NotesView
            implements NotesContract.View,
            NotesAdapter.IOnItemClickListener {
        private NotesContract.Presenter presenter;

        private RecyclerView notesRecycler;
        private NotesAdapter adapter;
        private TextView emptyPlaceholder;

        private boolean selectedMenuShown = false;

        private final Context context = NotesActivity.this;

        public void onCreate(@Nullable Bundle savedInstanceState) {
            setContentView(R.layout.activity_notes);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.createNewNote();
                }
            });

            notesRecycler = (RecyclerView) findViewById(R.id.recycler_notes);

            LinearLayoutManager liman = new LinearLayoutManager(context);
            notesRecycler.setLayoutManager(liman);

            adapter = new NotesAdapter(context);
            adapter.setOnItemClickListener(this);
            notesRecycler.setAdapter(adapter);

            emptyPlaceholder = (TextView) findViewById(R.id.text_empty_placeholder);
        }

        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_notes, menu);
            return true;
        }

        public boolean onPrepareOptionsMenu(Menu menu) {
            menu.clear();
            if (selectedMenuShown) {
                getMenuInflater().inflate(R.menu.menu_notes_selected, menu);
            } else {
                getMenuInflater().inflate(R.menu.menu_notes, menu);
            }
            return true;
        }

        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();

            switch (id) {
                case R.id.action_settings:
                    return true;
                case R.id.action_clear:
                    presenter.unSelectNotes();
                    return true;
                case R.id.action_delete:
                    presenter.deleteNotes();
                    return true;
                default:
                    return true;
            }
        }

        @Override
        public void onLongClick(int position) {
            if (!selectedMenuShown) {
                presenter.toggleNoteSelection(position);
            }
        }

        @Override
        public void onClick(int position) {
            if (selectedMenuShown) {
                presenter.toggleNoteSelection(position);
            } else {
                presenter.openNoteDetails(position);
            }
        }

        @Override
        public void showNotes(List<DecoratedNote> notes) {
            adapter.replaceNotes(notes);

            if (presenter.getDataSetSize() == 0) {
                emptyPlaceholder.setVisibility(View.VISIBLE);
                notesRecycler.setVisibility(View.GONE);
            } else {
                emptyPlaceholder.setVisibility(View.GONE);
                notesRecycler.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void showSelectedCount(int counterValue, Integer position) {
            if (counterValue > 0) {
                // TODO: use string resource
                setTitle("Выбрано: " + counterValue);
                if (!selectedMenuShown)
                    invalidateOptionsMenu();
                selectedMenuShown = true;
            } else {
                String label = "Notes";
                try {
                    PackageManager pm = getPackageManager();
                    ComponentName componentName = getComponentName();
                    ActivityInfo activityInfo = pm.getActivityInfo(componentName, 0);
                    label = activityInfo.loadLabel(pm).toString();
                } catch (PackageManager.NameNotFoundException ex) {
                    Log.e(NotePresenter.class.getSimpleName(), ex.getLocalizedMessage());
                }
                setTitle(label);
                if (selectedMenuShown)
                    invalidateOptionsMenu();
                selectedMenuShown = false;
            }

            if (position == null) {
                adapter.notifyDataSetChanged();
            } else {
                adapter.notifyItemChanged(position);
            }

            if (presenter.getDataSetSize() == 0) {
                emptyPlaceholder.setVisibility(View.VISIBLE);
                notesRecycler.setVisibility(View.GONE);
            }
        }

        @Override
        public void showNoteDetails(int nid) {
            Intent intent = new Intent(context, EditActivity.class);
            intent.putExtra(EditActivity.EXTRA_EDITABLE, false);
            intent.putExtra(EditActivity.EXTRA_NID, nid);
            context.startActivity(intent);
        }

        @Override
        public void showCreateNote() {
            Intent intent = new Intent(context, EditActivity.class);
            intent.putExtra(EditActivity.EXTRA_EDITABLE, true);
            context.startActivity(intent);
        }

        @Override
        public void setPresenter(@NonNull NotesContract.Presenter presenter) {
            this.presenter = presenter;
        }
    }

}
