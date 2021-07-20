package com.example.passmanager.model;

import android.net.Uri;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.passmanager.R;

@Entity(tableName = "Categories")
public class Category {

    /**
     * Category ID in the Password Manager.
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "categoryId")
    public int categoryId;

    /**
     * Category name.
     */
    @ColumnInfo(name = "name")
    public String name;

    /**
     * URI of an icon attached to this category.
     */
    @ColumnInfo(name = "icon")
    public Uri icon;

    public Category(String name, Uri icon) {
        this.name = name;
        this.icon = icon;
    }

    // TODO: Move this elsewhere?
    @Ignore
    private static final String uriHeader = "android.resource://com.example.passmanager/";

    // Default categories in the Password Manager.
    @Ignore
    public static final Category[] defaultCategories = {
            // TODO: Extract categories names string resources.
            new Category("Others",
                    Uri.parse(uriHeader + R.drawable.ic_baseline_others_24)),
            new Category("Streaming",
                    Uri.parse(uriHeader + R.drawable.ic_baseline_streaming_tv_24)),
            new Category("Social Media",
                    Uri.parse(uriHeader + R.drawable.ic_baseline_social_media_24)),
            new Category("Shopping",
                    Uri.parse(uriHeader + R.drawable.ic_baseline_shop_24)),
            new Category("Gaming",
                    Uri.parse(uriHeader + R.drawable.ic_baseline_gaming_24)),
            new Category("Gambling",
                    Uri.parse(uriHeader + R.drawable.ic_baseline_gambling_24)),
            new Category("Wifi",
                    Uri.parse(uriHeader + R.drawable.ic_baseline_wifi_24)),
            new Category("Email",
                    Uri.parse(uriHeader + R.drawable.ic_baseline_email_24)),
            new Category("Forum",
                    Uri.parse(uriHeader + R.drawable.ic_baseline_forum_24)),
            new Category("Bank",
                    Uri.parse(uriHeader + R.drawable.ic_baseline_bank_24))
    };
}
