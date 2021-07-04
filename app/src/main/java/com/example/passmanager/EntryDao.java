package com.example.passmanager;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * DAO interface for SQL Queries.
 *
 * <p>This DAO interface is used for specifying SQL queries on the "Entry" table and for associating
 * them with method calls.
 *    ListenableFuture and LiveData are used in order to make asynchronous queries.
 *    More details here: https://developer.android.com/training/data-storage/room/async-queries
 * </p>
 */
@Dao
public interface EntryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    ListenableFuture<Long> insert(Entry e);

    @Query("DELETE FROM entries;")
    ListenableFuture<Integer> deleteAllEntries();

    @Query("SELECT * FROM entries;")
    LiveData<List<Entry>> getAllEntries();



    // These methods are used for testing and they do not implicitly create an asynchronous thread.
    // They must be explicitly called from a background thread.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void simpleInsert(Entry e);

    @Query("SELECT * FROM entries;")
    List<Entry> simpleGetAllEntries();

}
