package com.example.passmanager.model;

import android.net.Uri;

import androidx.room.TypeConverter;

public class Converters {
    @TypeConverter
    public static Uri fromUriString(String uriStr) {
        return uriStr == null ? null : Uri.parse(uriStr);
    }

    @TypeConverter
    public static String toUriString(Uri uri) { return uri == null ? null : uri.toString(); }
}
