package com.example.passmanager.model;

import android.net.Uri;

import androidx.room.TypeConverter;

import java.sql.Date;

public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Uri fromUriString(String uriStr) {
        return uriStr == null ? null : Uri.parse(uriStr);
    }

    @TypeConverter
    public static String toUriString(Uri uri) { return uri == null ? null : uri.toString(); }
}
