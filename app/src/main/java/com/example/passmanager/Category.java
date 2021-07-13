package com.example.passmanager;

import android.net.Uri;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Categories")
public class Category {

    /**
     * Category number.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "categoryNo")
    public int categoryNo;

    @ColumnInfo(name = "name")
    public String name;

    /**
     * URI of an icon associated with the category.
     */
    @ColumnInfo(name = "icon")
    public Uri icon;

    public Category(String name, Uri icon) {
        this.name = name;
        this.icon = icon;
    }
}
