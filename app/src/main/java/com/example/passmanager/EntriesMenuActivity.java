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

public class EntriesMenuActivity extends AppCompatActivity implements EntryListAdapter.OnEntryListener {

    private AppBarConfiguration appBarConfiguration;
    private ActivityEntriesMenuBinding binding;
    private EntryViewModel entryVm;

    // TODO: Remove the hardcoded package name.
    public static final String EXTRA_ENTRY_ID = "com.example.passmanager.ENTRY_ID";
    private List<Entry> entriesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityEntriesMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_entries_menu);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(view -> {
            Intent intent = new Intent(this, CreateEntryActivity.class);
            startActivity(intent);
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        final EntryListAdapter adapter = new EntryListAdapter(this, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        entryVm = new ViewModelProvider(this).get(EntryViewModel.class);
        LiveData<List<Entry>> entries = entryVm.getAllEntries();
        if (entries != null) {
            entries.observe(this, entries1 -> {
                adapter.setEntries(entries1);
                entriesList = entries1;
            });
        }

    }

    @Override
    public void onEntryClick(int position) {
        int entryId = entriesList.get(position).entryNo;
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
        super.onDestroy();
        entryVm.close();
    }
}