package com.example.passmanager.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    ListenableFuture<List<Long>> insertCategories(Category... categories);

    @Query("SELECT * from Categories")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM Categories WHERE categoryNo = :categoryId")
    LiveData<Category> getCategory(int categoryId);

    @Update
    ListenableFuture<Integer> updateCategory(Category category);

    @Delete
    ListenableFuture<Integer> deleteCategory(Category category);

    /**
     * Get all categories and their entries.
     */
    @Transaction
    @Query("SELECT * from Categories")
    LiveData<List<CategoryWithEntries>> getCategoriesWithEntries();
}
