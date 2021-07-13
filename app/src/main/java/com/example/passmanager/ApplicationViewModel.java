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
 *
 * TODO: Refactor the class name.
 */
public class ApplicationViewModel extends AndroidViewModel {

    private final ApplicationRepository repository;
    private boolean validMasterPass;

    public ApplicationViewModel(@NonNull Application application) {
        super(application);
        repository = ApplicationRepository.getRepository();
    }

    /**
     * Open a connection with the data repository.
     *
     * @param application Current application context.
     * @param masterPass Master password for decrypting the repository data.
     * @param clearPass If true, clear the password from memory after opening the connection.
     */
    public void open(@NonNull Application application, char[] masterPass, boolean clearPass) {
        try {
            repository.open(application, masterPass, clearPass);
            validMasterPass = true;
        } catch (SQLiteException e) {
            validMasterPass = false;
        }
    }

    /**
     * Close the connection with the data repository.
     */
    public void close() {
        repository.close();
    }

    /**
     * Create a new data repository.
     *
     * @param application Current application context.
     * @param masterPass Master password for encrypting the data repository.
     * @param clearPass If true, clear the password from memory after opening the connection.
     */
    public void create(@NonNull Application application, char[] masterPass, boolean clearPass) {
        repository.create(application, masterPass, clearPass);
    }

    public boolean isValidMasterPass() { return validMasterPass; }

    // Queries
    public LiveData<List<Entry>> getAllEntries() { return repository.getAllEntries(); }
    public ListenableFuture<Long> insertEntry(Entry e) { return repository.insertEntry(e); }
    public LiveData<Entry> getEntry(int id) { return repository.getEntry(id); }
    public ListenableFuture<Integer> deleteEntry(Entry e) { return repository.deleteEntry(e); }
    public ListenableFuture<Integer> updateEntry(Entry e) { return repository.updateEntry(e); }

    // Categories queries
    public ListenableFuture<List<Long>> insertCategories(Category... c) { return repository.insertCategories(c); }
    public LiveData<List<Category>> getAllCategories() { return repository.getAllCategories(); }
}
