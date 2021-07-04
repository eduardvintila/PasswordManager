package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private TextView validationMsg;
    private EditText masterPassField;
    private EntryViewModel entryVm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        validationMsg = findViewById(R.id.validationTextView);
        masterPassField = findViewById(R.id.editTextPassword);

        entryVm = new ViewModelProvider(this).get(EntryViewModel.class);

        File databaseFile = getDatabasePath(EntryRoomDatabase.TABLE_NAME);
        if (!databaseFile.exists()) {
            goToCreate(null);
        }
    }

    public void auth(View view) {
        char[] pass = masterPassField.getText().toString().toCharArray();
        entryVm.open(getApplication(), pass);

        if (!entryVm.isValidMasterPass()) {
            validationMsg.setText(R.string.invalidPassMsg);
        } else {
            validationMsg.setText(R.string.validPassMsg);
        }
    }

    public void goToCreate(View view) {
        Intent intent = new Intent(this, CreateActivity.class);
        startActivity(intent);
    }
}