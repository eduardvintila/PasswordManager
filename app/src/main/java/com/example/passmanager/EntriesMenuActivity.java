package com.example.passmanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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
            // Get the encrypted master password from the authentication menu.
            Intent prevIntent = getIntent();
            String encryptedMaster = prevIntent.getStringExtra(AuthActivity.EXTRA_ENCRYPTED_MASTER);

            // Go to the create entry menu and pass the encrypted master password.
            Intent intent = new Intent(this, CreateOrUpdateEntryActivity.class);
            intent.putExtra(AuthActivity.EXTRA_ENCRYPTED_MASTER, encryptedMaster);
            startActivity(intent);
        });

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

    /**
     * Called when an entry from the RecyclerView adapter list has been clicked.
     *
     * @param position of the entry in the list.
     */
    @Override
    public void onEntryClick(int position) {
        // Get the encrypted master password from the authentication menu.
        Intent prevIntent = getIntent();
        String encryptedMaster = prevIntent.getStringExtra(AuthActivity.EXTRA_ENCRYPTED_MASTER);

        // Get the entry id.
        int entryId = adapter.getEntries().get(position).entryNo;

        // Go to the view entry details menu and pass the encrypted master password.
        Intent intent = new Intent(this, EntryActivity.class);
        intent.putExtra(EXTRA_ENTRY_ID, entryId);
        intent.putExtra(AuthActivity.EXTRA_ENCRYPTED_MASTER, encryptedMaster);
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

    public void goToCreateCategory(View view) {
        Intent intent = new Intent(this, CreateOrUpdateCategory.class);
        startActivity(intent);
    }
}