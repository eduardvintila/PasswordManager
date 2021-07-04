package com.example.passmanager;

import android.app.Application;
import android.content.Context;

import androidx.room.Room;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Various tests which check the connectivity with the Database.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseTests {

    private EntryDao entryDao;
    private EntryRoomDatabase db;

    @Before
    public void createDatabase() {

        Context context = ApplicationProvider.getApplicationContext();

        char[] passchrs = "123456".toCharArray();
        final byte[] pass = SQLiteDatabase.getBytes(passchrs);
        final SupportFactory factory = new SupportFactory(pass);

        // Create the database in memory.
        db = Room.inMemoryDatabaseBuilder(context, EntryRoomDatabase.class)
                .openHelperFactory(factory)
                .build();
        entryDao = db.entryDao();
    }

    @After
    public void closeDatabase() {
        db.close();
    }

    @Test
    public void insertTest() {

        Entry e0 = new Entry(1, "Yahoo", "Email Password", "/", "mail.yahoo.com", "test",
                "123456", "xyz123");
        Entry e1 = new Entry(2, "Google Drive", "Drive Password", "/", "drive.google.com", "test",
                "abcdef123", "rraa23");
        Entry e2 = new Entry(3, "Amazon", "Shop Password", "/", "amazon.com", "test",
                "xyz987", "x6546d");

        entryDao.simpleInsert(e0);
        entryDao.simpleInsert(e1);
        entryDao.simpleInsert(e2);

        List<Entry> entries = entryDao.simpleGetAllEntries();
        if (entries != null) {
            if (!e0.entryName.equals(entries.get(0).entryName)) { Assert.fail(); }
            if (!e1.entryName.equals(entries.get(1).entryName)) { Assert.fail(); }
            if (!e2.entryName.equals(entries.get(2).entryName)) { Assert.fail(); }

            // The test has passed if all entries' names are matched.
            Assert.assertTrue(true);
        } else {
            Assert.fail();
        }

    }
}
