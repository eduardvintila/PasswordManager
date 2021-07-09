package com.example.passmanager;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import net.sqlcipher.database.SQLiteException;

import java.util.List;

/**
 * ViewModel for passing data to the UI.
 */
public class EntryViewModel extends AndroidViewModel {

    private final EntryRepository entryRep;
    private boolean validMasterPass;

    public EntryViewModel(@NonNull Application application) {
        super(application);
        entryRep = EntryRepository.getRepository();
    }

    /**
     * Open a connection with the data repository.
     *
     * @param application Current application context.
     * @param masterPass Master password for decrypting the repository data.
     */
    public void open(@NonNull Application application, char[] masterPass) {
        try {
            entryRep.open(application, masterPass);
            validMasterPass = true;
        } catch (SQLiteException e) {
            validMasterPass = false;
        }
    }

    /**
     * Close the connection with the data repository.
     */
    public void close() {
        entryRep.close();
    }

    /**
     * Create a new data repository.
     *
     * @param application Current application context.
     * @param masterPass Master password for encrypting the data repository
     */
    public void create(@NonNull Application application, char[] masterPass) {
        entryRep.create(application, masterPass);
    }

    public boolean isValidMasterPass() { return validMasterPass; }

    // Queries
    public LiveData<List<Entry>> getAllEntries() { return entryRep.getAllEntries(); }
    public ListenableFuture<Long> insert(Entry e) { return entryRep.insert(e); }
    public LiveData<Entry> getEntry(int id) { return entryRep.getEntry(id); }
    public ListenableFuture<Integer> deleteEntry(Entry e) { return entryRep.deleteEntry(e); }
    public ListenableFuture<Integer> updateEntry(Entry e) { return entryRep.updateEntry(e); }
}
