package com.example.passmanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.passmanager.databinding.ActivityEntriesMenuBinding;

import java.util.List;

/**
 * Activity where each entry in the Password Manager is listed.
 */
public class EntriesMenuActivity extends AppCompatActivity implements EntryListAdapter.OnEntryListener {

    private AppBarConfiguration appBarConfiguration;
    private ActivityEntriesMenuBinding binding;
    private ApplicationViewModel viewmodel;

    // Identifier for passing the entry id in an intent to another activity.
    public static final String EXTRA_ENTRY_ID = BuildConfig.APPLICATION_ID + ".ENTRY_ID";

    private EntryListAdapter adapter;
    private String encryptedMaster;

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

        // Get the encrypted master password.
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file),
                Context.MODE_PRIVATE);
        encryptedMaster = sharedPref.getString(getString(R.string.encrypted_master), null);

        // FAB Listener for adding an entry.
        binding.fab.setOnClickListener(view -> {
            // Go to the create entry menu and pass the encrypted master password.
            Intent intent = new Intent(this, CreateOrUpdateEntryActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.addCategoryBtn).setOnClickListener(view -> goToCreateCategory());
        findViewById(R.id.modifyMasterPassBtn).setOnClickListener(view -> modifyMasterPass());

        // Setup the recycler view and it's adapter for populating entries
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        adapter = new EntryListAdapter(this, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get the entries from the ViewModel.
        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);
        LiveData<List<Entry>> entries = viewmodel.getAllEntries();
        if (entries != null) {
            entries.observe(this, adapter::setEntries);
        }

        viewmodel.getAllCategories().observe(this, categories -> {
            for (Category cat : categories) {
                // TODO: Remove this test.
                Log.d("CategoriesTest", cat.name);
            }
        });
    }

    public void modifyMasterPass() {
        Intent intent = new Intent(this, UpdateMasterPassActivity.class);
        startActivity(intent);
    }

    /**
     * Called when an entry from the RecyclerView adapter list has been clicked.
     *
     * @param position of the entry in the list.
     */
    @Override
    public void onEntryClick(int position) {
        // Get the entry id.
        int entryId = adapter.getEntries().get(position).entryNo;

        // Go to the view entry details menu and pass the encrypted master password.
        Intent intent = new Intent(this, EntryActivity.class);
        intent.putExtra(EXTRA_ENTRY_ID, entryId);
        startActivity(intent);
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
}