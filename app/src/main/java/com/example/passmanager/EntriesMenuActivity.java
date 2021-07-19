package com.example.passmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.passmanager.adapters.CategoryListAdapter;
import com.example.passmanager.databinding.ActivityEntriesMenuBinding;
import com.example.passmanager.dialogs.DeleteCategoryDialogFragment;
import com.example.passmanager.model.Category;
import com.example.passmanager.model.CategoryWithEntries;
import com.example.passmanager.model.Entry;
import com.example.passmanager.viewmodel.ApplicationViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity where each category in the Password Manager is listed, along with their entries.
 */
public class EntriesMenuActivity extends AppCompatActivity implements
        CategoryListAdapter.OnEntryClickListener, CategoryListAdapter.OnCategoryLongClickListener {


    private AppBarConfiguration appBarConfiguration;
    private ActivityEntriesMenuBinding binding;
    private ApplicationViewModel viewmodel;

    // Identifier for passing the entry id in an intent to another activity.
    public static final String EXTRA_ENTRY_ID = BuildConfig.APPLICATION_ID + ".ENTRY_ID";

    public static final String EXTRA_CATEGORY_ID = BuildConfig.APPLICATION_ID + ".CATEGORY_ID";

    private CategoryListAdapter adapter;

    // Lists of entries for each category.
    List<List<Entry>> entriesLists;

    // List of categories.
    List<Category> categories;

    // Callback and actionmode for displaying a contextual action bar.
    private CategoryActionModeCallback callback;
    private ActionMode actionMode;

    private View selectedCategoryBackgroundView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEntriesMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.topAppBar);

        // Navigation support.
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_entries_menu);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        // FAB Listener for adding an entry.
        binding.fab.setOnClickListener(view -> {
            // Go to the create entry menu and pass the encrypted master password.
            Intent intent = new Intent(this, CreateOrUpdateEntryActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.addCategoryBtn).setOnClickListener(view -> createCategory());
        findViewById(R.id.modifyMasterPassBtn).setOnClickListener(view -> modifyMasterPass());

        // Setup the recycler view and it's adapter for populating categories.
        RecyclerView recyclerView = findViewById(R.id.categoriesRecyclerView);
        adapter = new CategoryListAdapter(this, this, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);
        viewmodel.getCategoriesWithEntries().observe(this, categoriesWithEntries -> {
            if (categoriesWithEntries != null) {
                entriesLists = new ArrayList<>();
                categories = new ArrayList<>();
                for (CategoryWithEntries catWithEntries : categoriesWithEntries) {
                    categories.add(catWithEntries.category);
                    entriesLists.add(catWithEntries.entries);
                    adapter.setCategories(categories);
                    adapter.setEntriesLists(entriesLists);
                }
            }
        });

        callback = new CategoryActionModeCallback();
    }


    /**
     * Inflate the top menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.top_app_bar, menu);

        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_entries_menu);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        // TODO: This might not execute at all!
        // Close the connection with the data repository, since (most likely?) this activity is
        // destroyed when the application is terminated.
        super.onDestroy();
        viewmodel.close();
    }

    /**
     * Go to the create category menu.
     */
    public void createCategory() {
        Intent intent = new Intent(this, CreateOrUpdateCategoryActivity.class);
        startActivity(intent);
    }

    /**
     * Go to the edit category menu.
     * @param categoryIndex the index in the categories list.
     */
    public void editCategory(int categoryIndex) {
        int categoryId = categories.get(categoryIndex).categoryNo;
        Intent intent = new Intent(this, CreateOrUpdateCategoryActivity.class);
        intent.putExtra(EntriesMenuActivity.EXTRA_CATEGORY_ID, categoryId);
        startActivity(intent);
        actionMode.finish();
    }

    /**
     * Load a confirmation dialog box for deleting the category.
     * @param categoryIndex the index in the categories list.
     */
    public void loadDeleteCategoryDialog(int categoryIndex) {
        Category category = categories.get(categoryIndex);

        if (category.categoryNo == 1) {
            // Don't delete the "Others" category. This category is used for relocating the
            // entries in the deleted categories.
            Toast.makeText(this, R.string.cannot_delete_category, Toast.LENGTH_SHORT).show();
        } else {
            DialogFragment dialogFragment =
                    new DeleteCategoryDialogFragment(category, (dialog, which) -> {
                        // Executed when the positive button has been presed.
                        viewmodel.deleteCategory(category);
                        actionMode.finish();
                    });
            dialogFragment.show(getSupportFragmentManager(), "dialogDeleteCategory");
        }
    }

    /**
     * Go to the modify master password menu.
     */
    public void modifyMasterPass() {
        Intent intent = new Intent(this, UpdateMasterPassActivity.class);
        startActivity(intent);
    }

    /**
     * Called when an entry from the RecyclerView adapter list has been clicked.
     *
     * @param categoryIndex index in the categories adapter list
     * @param entryIndex index in the list of entries of a category.
     */
    @Override
    public void onEntryClick(int categoryIndex, int entryIndex) {
        int entryId = entriesLists.get(categoryIndex).get(entryIndex).entryNo;
        // Go to the view entry details menu.
        Intent intent = new Intent(this, EntryActivity.class);
        intent.putExtra(EXTRA_ENTRY_ID, entryId);
        startActivity(intent);
    }

    /**
     * Called when a category is long clicked. Displays a contextual action bar for
     * modifying/deleting the category.
     *
     * @param categoryIndex the index in the categories list.
     */
    @Override
    public void onLongClick(int categoryIndex, View categoryBackgroundView) {
        callback.setCategoryIndex(categoryIndex);
        selectedCategoryBackgroundView = categoryBackgroundView;
        if (actionMode == null) {
            // Display a contextual action bar if it doesn't already exist.
            actionMode = startSupportActionMode(callback);
            actionMode.setTitle(getString(R.string.category_selected));
        }

    }

    /**
     * Callback for the contextual action bar.
     */
    private class CategoryActionModeCallback implements ActionMode.Callback {

        // Save the index of the category that is processed.
        int categoryIndex;

        public void setCategoryIndex(int categoryIndex) {
            this.categoryIndex = categoryIndex;
        }

        /**
         * Inflate the menu buttons of the contextual action bar.
         */
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            getMenuInflater().inflate(R.menu.contextual_action_bar, menu);
            selectedCategoryBackgroundView.setBackgroundColor(getColor(android.R.color.darker_gray));
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.deleteCategoryBtn:
                    loadDeleteCategoryDialog(categoryIndex);
                    return true;

                case R.id.modifyCategoryBtn:
                    editCategory(categoryIndex);
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectedCategoryBackgroundView.setBackgroundColor(getColor(android.R.color.white));
            actionMode = null;
        }
    }
}