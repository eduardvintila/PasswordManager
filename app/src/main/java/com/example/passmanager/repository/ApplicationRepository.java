package com.example.passmanager.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.passmanager.model.ApplicationDatabase;
import com.example.passmanager.model.Category;
import com.example.passmanager.model.CategoryWithEntries;
import com.example.passmanager.model.Entry;
import com.google.common.util.concurrent.ListenableFuture;

import net.sqlcipher.database.SQLiteException;

import java.io.File;
import java.util.List;

import com.example.passmanager.model.CategoryDao;
import com.example.passmanager.model.EntryDao;

/**
 * Singleton repository to access the database operations.
 */
public class ApplicationRepository {

    private static ApplicationRepository INSTANCE;

    private ApplicationDatabase db;
    private EntryDao entryDao;
    private CategoryDao categoryDao;

    /**
     * Cache all entries.
     */
    private LiveData<List<Entry>> allEntries;

    private ApplicationRepository() {}

    public static ApplicationRepository getRepository() {
        if (INSTANCE == null) {
            INSTANCE = new ApplicationRepository();
        }
        return INSTANCE;
    }


    /**
     * Open a connection with the local database.
     *
     * @param application Current application context
     * @param masterPass Master password for decrypting the database.
     * @param clearPass If true, clear the password from memory after opening the connection.
     * @return true if the connection has been opened successfully; false otherwise.
     */
    public boolean open(Application application, char[] masterPass, boolean clearPass) {
        try {
            db = ApplicationDatabase.getDatabase(application, masterPass, clearPass);
            entryDao = db.entryDao();
            categoryDao = db.categoryDao();
            allEntries = entryDao.getAllEntries();
            return true;
        } catch (SQLiteException e) {
            return false;
        }
    }

    /**
     * Close the connection with the local database.
     */
    public void close() {
        db.closeDatabase();
        db = null;
    }

    public void changeMasterPassword(char[] newPassword) {
        db.changeMasterPassword(newPassword);
    }


    /**
     * Create a new local database. Delete the previous one if it exists.
     *
     * @param application Current application context.
     * @param masterPass Master password for encrypting the database
     * @param clearPass If true, clear the password from memory after opening the connection.
     */
    public void create(Application application, char[] masterPass, boolean clearPass) {
        delete(application);
        open(application, masterPass, clearPass);
    }

    /**
     * Delete the local database.
     * @param application Current application context.
     */
    public void delete(Application application) {
        File databaseFile = application.getDatabasePath(ApplicationDatabase.DB_NAME);
        databaseFile.mkdirs();
        databaseFile.delete();
    }

    // Entries queries
    public LiveData<List<Entry>> getAllEntries() { return allEntries; }
    public ListenableFuture<Long> insertEntry(Entry e) { return entryDao.insert(e); }
    public LiveData<Entry> getEntry(int id) { return entryDao.getEntry(id); }
    public ListenableFuture<Integer> deleteEntry(Entry e) { return entryDao.deleteEntry(e); }
    public ListenableFuture<Integer> updateEntry(Entry e) { return entryDao.updateEntry(e); }
    public ListenableFuture<Integer> updateEntries(List<Entry> entries) { return entryDao.updateEntries(entries); }

    // Categories queries
    public ListenableFuture<List<Long>> insertCategories(Category... c) { return categoryDao.insertCategories(c); }
    public LiveData<List<Category>> getAllCategories() { return categoryDao.getAllCategories(); }
    public LiveData<Category> getCategory(int categoryId) { return categoryDao.getCategory(categoryId); }
    public ListenableFuture<Integer> updateCategory(Category category) { return categoryDao.updateCategory(category); }
    public ListenableFuture<Integer> deleteCategory(Category category) { return categoryDao.deleteCategory(category); }
    public LiveData<List<CategoryWithEntries>> getCategoriesWithEntries() { return categoryDao.getCategoriesWithEntries(); }
}
