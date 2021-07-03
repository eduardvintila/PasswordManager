package com.example.passmanager;

import androidx.lifecycle.LiveData;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO interface for SQL Queries.
 *
 * <p>This DAO interface is used for specifying SQL queries on the "Entry" table and for associating
 * them with method calls.
 * </p>
 */
public interface EntryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Entry e);

    @Query("DELETE FROM entries;")
    void deleteAllEntries();

    @Query("SELECT * FROM entries;")
    LiveData<List<Entry>> getAllEntries();

}
