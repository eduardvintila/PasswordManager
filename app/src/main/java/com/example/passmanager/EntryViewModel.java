package com.example.passmanager;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * ViewModel for passing data to the UI.
 */
public class EntryViewModel extends AndroidViewModel {

    private EntryRepository entryRep;

    /**
     * Cache all entries.
     */
    private LiveData<List<Entry>> allEntries;

    public EntryViewModel(@NonNull Application application) {
        super(application);
        entryRep = new EntryRepository(application);
        allEntries = entryRep.getAllEntries();
    }

    public LiveData<List<Entry>> getAllEntries() { return allEntries; }

    public ListenableFuture<Long> insert(Entry e) { return entryRep.insert(e); }
}
