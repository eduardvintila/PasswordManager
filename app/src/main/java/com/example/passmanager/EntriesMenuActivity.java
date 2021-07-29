package com.example.passmanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import androidx.appcompat.view.ActionMode;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.passmanager.adapters.CategoryListAdapter;
import com.example.passmanager.databinding.ActivityEntriesMenuBinding;
import com.example.passmanager.model.Category;
import com.example.passmanager.model.CategoryWithEntries;
import com.example.passmanager.model.Entry;
import com.example.passmanager.utils.DriveHelper;
import com.example.passmanager.utils.NetworkHelper;
import com.example.passmanager.viewmodel.ApplicationViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDateTime;
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

    // Callback and actionmode for displaying a contextual action bar when a category is long
    // clicked.
    private CategoryActionModeCallback callback;
    private ActionMode actionMode;

    private View selectedCategoryBackgroundView;

    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private DriveHelper driveHelper;

    private SharedPreferences sharedPref;

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

        driveHelper = DriveHelper.getInstance();
        googleSignInLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                // Pass the sign in result to the DriveHelper.
                                driveHelper.onSignInResult(result.getData(), this);
                            }
                        });

        // Listener for clicks on the action bar items.
        binding.topAppBar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.addCategoryBtn:
                    createCategory();
                    return true;

                case R.id.modifyMasterPassBtn:
                    modifyMasterPass();
                    return true;

                case R.id.syncDbBtn:
                    if (NetworkHelper.isInternetConnectionAvailable(this)) {
                        driveHelper.signIn(googleSignInLauncher, this);
                        driveHelper.showDriveDbSyncDialog(this, (success -> {
                            // Upload finished.
                            if (success) {
                                Toast.makeText(this, R.string.upload_successful,
                                        Toast.LENGTH_SHORT).show();
                            }  else {
                                Toast.makeText(this, R.string.upload_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }), (success) -> {
                            // Download finished.
                            if (success) {
                                Toast.makeText(this, R.string.download_successful_auth,
                                        Toast.LENGTH_LONG).show();
                                viewmodel.close();
                                Intent intent = new Intent(this, AuthActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(this, R.string.download_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, R.string.no_internet_connection,
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;

                case R.id.settingsBtn:
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                    return true;

                default:
                    return false;
            }
        });

        sharedPref = getSharedPreferences(getString(R.string.preference_file),
                Context.MODE_PRIVATE);
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

    @Override
    protected void onPause() {
        super.onPause();

        // Get the session expire minutes setting value.
        int expireMinutes = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(getString(R.string.session_expire_minutes_setting),
                        getResources().getInteger(R.integer.session_expire_default_minutes));

        // When the main menu leaves the foreground, set an expiration time after which a new
        // authentication is needed.
        String expireTimeStr = LocalDateTime.now().plusMinutes(expireMinutes).toString();
        sharedPref.edit()
                .putString(getString(R.string.session_expire_time_key), expireTimeStr)
                .apply();
    }

    @Override
    protected void onResume() {
        super.onResume();

        String expireTimeStr = sharedPref.getString(
                getString(R.string.session_expire_time_key), null);
        if (expireTimeStr != null) {
            // Check if the current session has expired.
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime expireTime = LocalDateTime.parse(expireTimeStr);
            if (currentTime.isAfter(expireTime)) {
                // Session expired.
                viewmodel.close();
                Toast.makeText(this, R.string.session_expired, Toast.LENGTH_LONG).show();

                // Go to the authentication menu.
                Intent intent = new Intent(this, AuthActivity.class);
                startActivity(intent);
                finish();
            }
        }
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
        int categoryId = categories.get(categoryIndex).categoryId;
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

        if (category.categoryId == 1) {
            // Don't delete the "Others" category. This category is used for relocating the
            // entries in the deleted categories.
            Toast.makeText(this, R.string.cannot_delete_category, Toast.LENGTH_SHORT).show();
        } else {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_category)
                    .setMessage(String.format(getString(R.string.delete_category_format),
                            category.name))
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        viewmodel.deleteCategory(category);
                        actionMode.finish();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
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
        int entryId = entriesLists.get(categoryIndex).get(entryIndex).entryId;
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