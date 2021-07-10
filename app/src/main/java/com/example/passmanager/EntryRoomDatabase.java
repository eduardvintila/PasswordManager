package com.example.passmanager;

import android.content.Context;
import android.os.Debug;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteDatabaseHook;
import net.sqlcipher.database.SQLiteException;
import net.sqlcipher.database.SupportFactory;

import java.util.Arrays;
import java.util.Locale;

/**
 * Room singleton class for establishing connection with the SQLCipher database.
 */
@Database(entities = {Entry.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class EntryRoomDatabase extends RoomDatabase {

    private static EntryRoomDatabase INSTANCE;
    public static final String TABLE_NAME = "Entries";

    /**
     * Get the DAO for the "Entries" table.
     */
    public abstract EntryDao entryDao();

    /**
     * Get a handle to the RoomDatabase. Creates the database if it doesn't exist.
     *
     * @param context The current application context.
     */
    public static EntryRoomDatabase getDatabase(final Context context, char[] masterPass,
                                                boolean clearPass)
            throws SQLiteException {
        if (INSTANCE == null) {
            // Make sure that only one thread creates the handle to the database in order to prevent
            // a race condition.
            synchronized (EntryRoomDatabase.class) {
                if (INSTANCE == null) {
                    final byte[] masterPassBytes = SQLiteDatabase.getBytes(masterPass);
                    final SupportFactory factory = new SupportFactory(masterPassBytes);

                    INSTANCE = Room.databaseBuilder(context, EntryRoomDatabase.class, TABLE_NAME)
                            .fallbackToDestructiveMigration()
                            .openHelperFactory(factory)
                            .build();
                    try {
                        // SQLCipher doesn't check if the master password is valid until a command is
                        // issued against the database. The statement below forces this validation and
                        // throws an exception if the key is not valid.
                        INSTANCE.getOpenHelper().getReadableDatabase();
                    } catch (SQLiteException e) {
                        closeDatabase();
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

    public static void closeDatabase() {
        INSTANCE.close();
        INSTANCE = null;
    }
}
