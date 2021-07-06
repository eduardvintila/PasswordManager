package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class EntryActivity extends AppCompatActivity {

    private TextView entryNameField;
    private TextView userIdField;
    private TextView userPasswordField;
    private TextView entryDescriptionField;
    private TextView serviceLinkField;
    private EntryViewModel entryVm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        entryVm = new ViewModelProvider(this).get(EntryViewModel.class);

        entryNameField = findViewById(R.id.entryNameTextView);
        userIdField = findViewById(R.id.userIdTextView);
        userPasswordField = findViewById(R.id.userPassTextView);
        entryDescriptionField = findViewById(R.id.entryDescriptionTextView);
        serviceLinkField = findViewById(R.id.serviceLinkTextView);

        Intent intent = getIntent();
        if (intent != null) {
            int entryId = intent.getIntExtra(EntriesMenuActivity.EXTRA_ENTRY_ID, 1);
            entryVm.getEntry(entryId).observe(this, entry -> {
                entryNameField.setText(entry.entryName);
                userIdField.setText(entry.userId);
                userPasswordField.setText(entry.userPassword);
                entryDescriptionField.setText(entry.entryDescription);
                serviceLinkField.setText(entry.serviceLink);
            });
        }

    }
}