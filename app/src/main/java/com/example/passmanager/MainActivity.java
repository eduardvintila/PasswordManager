package com.example.passmanager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private EntryViewModel entryVm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);


        // Placeholder tests for checking the database operations.
        ExecutorService executorService = Executors.newCachedThreadPool(); // Create thread pool
        entryVm = new ViewModelProvider(this).get(EntryViewModel.class);
        Entry entry = new Entry("Yahoo", "Parola mail", "/", "mail.yahoo.com", "test",
                "123456", "xyz123");
        ListenableFuture<Long> future = entryVm.insert(entry);
        future.addListener(() -> { Log.d("MainActivity", "Insertion successful.");},
                executorService); // Run listener callback on one of the threads from the pool

        entryVm.getAllEntries().observe(this, entries -> {
            if (entries != null) {
                StringBuilder text = new StringBuilder();
                for (Entry e : entries) {
                    text.append(e.entryName + ", ");
                }
                textView.setText(text);
            }
        });

        Entry e2 = new Entry("Google", "Parola mail", "/", "mail.yahoo.com", "test",
                "123456", "xyz123");
        entryVm.insert(e2);

        Entry e3 = new Entry("Amazon", "Parola mail", "/", "mail.yahoo.com", "test",
                "123456", "xyz123");
        entryVm.insert(e3);
    }
}