package com.example.passmanager;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * Repository to access the "Entries" table database operations.
 */
public class EntryRepository {

    private EntryDao entryDao;

    /**
     * Cache all entries.
     */
    private LiveData<List<Entry>> allEntries;

    public EntryRepository(Application application) {
        EntryRoomDatabase db = EntryRoomDatabase.getDatabase(application);
        entryDao = db.entryDao();
        allEntries = entryDao.getAllEntries();
    }

    public LiveData<List<Entry>> getAllEntries() { return allEntries; }

    public ListenableFuture<Long> insert(Entry e) { return entryDao.insert(e); }

}
