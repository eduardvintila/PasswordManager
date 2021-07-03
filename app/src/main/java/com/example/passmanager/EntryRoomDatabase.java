package com.example.passmanager;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

/**
 * Room singleton class for establishing connection with the SQLCipher database.
 */
@Database(entities = {Entry.class}, version = 1, exportSchema = false)
public abstract class EntryRoomDatabase extends RoomDatabase {

    private static EntryRoomDatabase INSTANCE;

    /**
     * Get the DAO for the "Entries" table.
     */
    public abstract EntryDao entryDao();

    /**
     * Get a handle to the RoomDatabase. Creates one if it isn't instantiated.
     * @param context The current application context.
     */
    public static EntryRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            // Make sure that only one thread creates the handle to the database in order to prevent
            // a race condition.
            synchronized (EntryRoomDatabase.class) {
                if (INSTANCE == null) {

                    // TODO: Change this placeholder test for the database connection.
                    char[] passchrs = "123456".toCharArray();
                    final byte[] pass = SQLiteDatabase.getBytes(passchrs);
                    final SupportFactory factory = new SupportFactory(pass);

                    // TODO: Change the hardcoded table name parameter.
                    INSTANCE = Room.databaseBuilder(context, EntryRoomDatabase.class, "Entries")
                            .fallbackToDestructiveMigration()
                            .openHelperFactory(factory)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
