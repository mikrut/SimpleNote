package ru.bmstu.iu6.simplenote.activities.notes;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
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
import ru.bmstu.iu6.simplenote.activities.adapters.IOnItemClickListener;
import ru.bmstu.iu6.simplenote.activities.login.LoginActivity;
import ru.bmstu.iu6.simplenote.activities.noteedit.EditActivity;
import ru.bmstu.iu6.simplenote.activities.settings.SettingsActivity;
import ru.bmstu.iu6.simplenote.data.database.NotesDAO;
import ru.bmstu.iu6.simplenote.data.source.NotesDataSource;
import ru.bmstu.iu6.simplenote.models.DecoratedNote;
import ru.bmstu.iu6.simplenote.activities.adapters.NotesAdapter;
import ru.bmstu.iu6.simplenote.data.source.NotesRepository;
import ru.bmstu.iu6.simplenote.threading.SchedulerProvider;

public class NotesActivity
        extends AppCompatActivity {
    private static final String SAVED_SELECTED =
            NotesActivity.class.getCanonicalName() + ".SAVED_SELECTED";

    private NotesView view = new NotesView();
    private NotesPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view.onCreate(savedInstanceState);

        // FIXME: use dependency injection
        NotesDataSource localSource = NotesDAO.getInstance(getApplicationContext());
        if (localSource == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        NotesRepository notesRepository = NotesRepository.getInstance(localSource);
        presenter = new NotesPresenter(view, SchedulerProvider.getInstance(), notesRepository);
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        view.onPause();
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
        //noinspection unchecked because we're sure about what we save
        Set<Integer> notes = (Set<Integer>) savedInstanceState.getSerializable(SAVED_SELECTED);

        if (notes != null) {
            presenter.restoreSelected(notes);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Set<Integer> decoratedNotes = presenter.filterSelected(false);
        outState.putSerializable(SAVED_SELECTED, (Serializable) decoratedNotes);
    }

    private class NotesView
            implements NotesContract.View,
            IOnItemClickListener {
        private NotesContract.Presenter presenter;

        private RecyclerView notesRecycler;
        private NotesAdapter adapter;
        private TextView emptyPlaceholder;

        private SearchView searchView;

        private boolean selectedMenuShown = false;

        private final Context context = NotesActivity.this;

        public void onCreate(@Nullable Bundle savedInstanceState) {
            setContentView(R.layout.activity_notes);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener((View view) -> presenter.createNewNote());

            notesRecycler = (RecyclerView) findViewById(R.id.recycler_notes);

            LinearLayoutManager liman = new LinearLayoutManager(context);
            notesRecycler.setLayoutManager(liman);

            adapter = new NotesAdapter(context);
            adapter.setOnItemClickListener(this);
            notesRecycler.setAdapter(adapter);

            emptyPlaceholder = (TextView) findViewById(R.id.text_empty_placeholder);
        }

        public void onResume() {
            presenter.start();
        }

        public void onPause() {
            presenter.unsubscribe();
        }

        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_notes, menu);
            initSearchView(menu);

            return true;
        }

        private void initSearchView(Menu menu){
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            final MenuItem searchItem = menu.findItem(R.id.action_search);
            searchView = (SearchView) searchItem.getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setIconifiedByDefault(false);

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    presenter.searchNotes(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText.equals("")) {
                        presenter.loadNotes(true);
                    } else {
                        presenter.searchNotes(newText);
                    }
                    return true;
                }
            });

            searchView.setOnCloseListener(() -> true);

            MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem menuItem) {
                    // Nothing
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                    Log.d("close", "close");
                    presenter.finishSearch();
                    return true;
                }
            });
        }

        public boolean onPrepareOptionsMenu(Menu menu) {
            menu.clear();
            if (selectedMenuShown) {
                getMenuInflater().inflate(R.menu.menu_notes_selected, menu);
            } else {
                getMenuInflater().inflate(R.menu.menu_notes, menu);
                initSearchView(menu);
            }
            return true;
        }

        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();

            switch (id) {
                case R.id.action_settings:
                    Intent intent = new Intent(NotesActivity.this, SettingsActivity.class);
                    startActivity(intent);
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
                    Log.e(NotesPresenter.class.getSimpleName(), ex.getLocalizedMessage());
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
