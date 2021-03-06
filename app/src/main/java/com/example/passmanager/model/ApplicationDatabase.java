package com.example.passmanager.model;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SupportFactory;

import java.util.Arrays;

/**
 * Room singleton class for establishing connection with the SQLCipher database.
 */
@Database(entities = {Entry.class, Category.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class ApplicationDatabase extends RoomDatabase {

    private static ApplicationDatabase INSTANCE;

    public static final String DB_NAME = "db";

    /**
     * DAO for the "Entries" table.
     */
    public abstract EntryDao entryDao();

    /**
     * DAO for the "Categories" table
     */
    public abstract CategoryDao categoryDao();

    /**
     * Get a handle to the RoomDatabase. Creates the database if it doesn't exist.
     *
     * @param context The current application context.
     */
    public static ApplicationDatabase getDatabase(final Context context, char[] masterPass,
                                                  boolean clearPass)
            throws SQLiteException {
        if (INSTANCE == null) {
            // Make sure that only one thread creates the handle to the database in order to prevent
            // a race condition.
            synchronized (ApplicationDatabase.class) {
                if (INSTANCE == null) {
                    final byte[] masterPassBytes = SQLiteDatabase.getBytes(masterPass);
                    final SupportFactory factory = new SupportFactory(masterPassBytes);

                    INSTANCE = Room.databaseBuilder(context, ApplicationDatabase.class, DB_NAME)
                            .fallbackToDestructiveMigration()
                            .openHelperFactory(factory)
                            .setJournalMode(JournalMode.TRUNCATE)
                            .build();
                    try {
                        // SQLCipher doesn't check if the master password is valid until a command is
                        // issued against the database. The statement below forces this validation and
                        // throws an exception if the key is not valid.
                        INSTANCE.getOpenHelper().getReadableDatabase();
                    } catch (SQLiteException e) {
                        INSTANCE.closeDatabase();
                        throw e;
                    } finally {
                        if (clearPass) {
                            // Clear the plaintext password from memory.
                            Arrays.fill(masterPass, (char) 0);
                            Arrays.fill(masterPassBytes, (byte) 0);
                        }
                    }
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Change the master password by making a query to the SQLCipher database. The database must
     * have been opened previously.
     *
     * @param newMasterPassword The new master password.
     */
    public void changeMasterPassword(char[] newMasterPassword) {
        SupportSQLiteDatabase db =  getOpenHelper().getWritableDatabase();
        String rekey = String.format("PRAGMA rekey='%s'", String.valueOf(newMasterPassword));
        db.query(rekey);
    }

    public void closeDatabase() {
        INSTANCE.close();
        INSTANCE = null;
    }
}
