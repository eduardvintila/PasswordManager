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

    private EntryRepository entryRep;
    private boolean validMasterPass;

    /**
     * Cache all entries.
     */
    private LiveData<List<Entry>> allEntries;


    public EntryViewModel(@NonNull Application application) {
        super(application);
        entryRep = new EntryRepository();
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
            allEntries = entryRep.getAllEntries();
            validMasterPass = true;
        } catch (SQLiteException e) {
            validMasterPass = false;
        }
    }

    public boolean isValidMasterPass() { return validMasterPass; }

    public LiveData<List<Entry>> getAllEntries() { return allEntries; }

    public ListenableFuture<Long> insert(Entry e) { return entryRep.insert(e); }
}
