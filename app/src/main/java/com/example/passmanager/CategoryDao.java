package com.example.passmanager;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    ListenableFuture<List<Long>> insertCategories(Category... categories);

    @Query("SELECT * from Categories")
    LiveData<List<Category>> getAllCategories();
}
