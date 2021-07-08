package com.example.passmanager;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import net.sqlcipher.database.SQLiteException;

import java.io.File;
import java.util.List;

/**
 * Singleton repository to access the "Entries" table database operations.
 */
public class EntryRepository {

    private static EntryRepository INSTANCE;

    private EntryRoomDatabase db;
    private EntryDao entryDao;

    /**
     * Cache all entries.
     */
    private LiveData<List<Entry>> allEntries;

    private EntryRepository() {}

    public static EntryRepository getRepository() {
        if (INSTANCE == null) {
            INSTANCE = new EntryRepository();
        }
        return INSTANCE;
    }


    /**
     * Open a connection with the local database.
     *
     * @param application Current application context
     * @param masterPass Master password for decrypting the database.
     * @throws SQLiteException if the connection with the database fails (most likely because the
     * master password is invalid).
     */
    public void open(Application application, char[] masterPass) throws SQLiteException {
        db = EntryRoomDatabase.getDatabase(application, masterPass);
        entryDao = db.entryDao();
        allEntries = entryDao.getAllEntries();
    }

    /**
     * Close the connection with the local database.
     */
    public void close() {
        EntryRoomDatabase.closeDatabase();
        db = null;
    }


    /**
     * Create a new local database. Deletes the previous one if it exists.
     *
     * @param application Current application context.
     * @param masterPass Master password for encrypting the database
     */
    public void create(Application application, char[] masterPass) {
        File databaseFile = application.getDatabasePath(EntryRoomDatabase.TABLE_NAME);
        databaseFile.mkdirs();
        databaseFile.delete();

        open(application, masterPass);
    }

    // Queries
    public LiveData<List<Entry>> getAllEntries() { return allEntries; }
    public ListenableFuture<Long> insert(Entry e) { return entryDao.insert(e); }
    public LiveData<Entry> getEntry(int id) { return entryDao.getEntry(id); }
    public ListenableFuture<Integer> deleteEntry(Entry e) { return entryDao.deleteEntry(e); }

}
