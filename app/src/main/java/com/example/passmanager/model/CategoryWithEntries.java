package com.example.passmanager.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/**
 * Models the "one-to-many" relationship between categories and entries. For each category, we
 * have a list of entries associated with it.
 */
public class CategoryWithEntries {
    @Embedded
    public Category category;

    @Relation(parentColumn = "categoryNo", entityColumn = "categoryNo")
    public List<Entry> entries;
}
