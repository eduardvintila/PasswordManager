package com.example.passmanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.passmanager.BuildConfig;
import com.example.passmanager.CreateOrUpdateCategoryActivity;
import com.example.passmanager.CreateOrUpdateEntryActivity;
import com.example.passmanager.EntryActivity;
import com.example.passmanager.R;
import com.example.passmanager.UpdateMasterPassActivity;
import com.example.passmanager.adapters.CategoryListAdapter;
import com.example.passmanager.databinding.ActivityEntriesMenuBinding;
import com.example.passmanager.model.Category;
import com.example.passmanager.model.CategoryWithEntries;
import com.example.passmanager.model.Entry;
import com.example.passmanager.viewmodel.ApplicationViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity where each category in the Password Manager is listed, along with their entries.
 */
public class EntriesMenuActivity extends AppCompatActivity implements CategoryListAdapter.OnEntryClickListener {

    private AppBarConfiguration appBarConfiguration;
    private ActivityEntriesMenuBinding binding;
    private ApplicationViewModel viewmodel;

    // Identifier for passing the entry id in an intent to another activity.
    public static final String EXTRA_ENTRY_ID = BuildConfig.APPLICATION_ID + ".ENTRY_ID";

    private CategoryListAdapter adapter;

    // Lists of entries for each category.
    List<List<Entry>> entriesLists;

    // List of categories.
    List<Category> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEntriesMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

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

        findViewById(R.id.addCategoryBtn).setOnClickListener(view -> goToCreateCategory());
        findViewById(R.id.modifyMasterPassBtn).setOnClickListener(view -> modifyMasterPass());

        // Setup the recycler view and it's adapter for populating categories.
        RecyclerView recyclerView = findViewById(R.id.categoriesRecyclerView);
        adapter = new CategoryListAdapter(this, this);
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
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_entries_menu);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        // Close the connection with the data repository, since (most likely?) this activity is
        // destroyed when the application is terminated.
        super.onDestroy();
        viewmodel.close();
    }

    public void goToCreateCategory() {
        Intent intent = new Intent(this, CreateOrUpdateCategoryActivity.class);
        startActivity(intent);
    }

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
        // Go to the view entry details menu and pass the encrypted master password.
        Intent intent = new Intent(this, EntryActivity.class);
        intent.putExtra(EXTRA_ENTRY_ID, entryId);
        startActivity(intent);
    }
}