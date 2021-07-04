package com.example.passmanager;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;

import java.io.File;
import java.util.List;

/**
 * Repository to access the "Entries" table database operations.
 */
public class EntryRepository {

    private EntryRoomDatabase db;
    private EntryDao entryDao;

    /**
     * Cache all entries.
     */
    private LiveData<List<Entry>> allEntries;


    /**
     * Open the connection with the local database.
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

    public void create(Application application, char[] masterPass) {
        // SQLiteDatabase.loadLibs(application);
        File databaseFile = application.getDatabasePath(EntryRoomDatabase.TABLE_NAME);
        databaseFile.mkdirs();
        databaseFile.delete();

        open(application, masterPass);
    }

    public LiveData<List<Entry>> getAllEntries() { return allEntries; }

    public ListenableFuture<Long> insert(Entry e) { return entryDao.insert(e); }


    public void close() {
        EntryRoomDatabase.closeDatabase();
        db = null;
    }
}
