package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private TextView validationMsg;
    private EditText masterPassField;
    private EntryViewModel entryVm;

    public static final String EXTRA_ENCRYPTED_MASTER = "com.example.passmanager.ENCRYPTED_MASTER";

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

    /**
     * Authenticate using the master password.
     */
    public void auth(View view) {
        // TODO: Make sure to clean the plaintext pass from memory.

        // Get the password from the input field.
        Editable editable = masterPassField.getText();
        char[] pass = new char[editable.length()];
        editable.getChars(0, editable.length(), pass, 0);

        if (pass.length == 0)
            return;

        entryVm.open(getApplication(), pass, false);
        if (!entryVm.isValidMasterPass()) {
            validationMsg.setText(R.string.invalid_pass);
        } else {
            // Encrypt the master password and pass it to the next activities in order to use it
            // for encrypting/decrypting passwords in the entries.
            String encrypted = CryptoHelper.encryptMasterPassword(pass);
            Intent intent = new Intent(this, EntriesMenuActivity.class);
            intent.putExtra(EXTRA_ENCRYPTED_MASTER, encrypted);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Switch to the create database menu.
     */
    public void goToCreate(View view) {
        Intent intent = new Intent(this, CreateActivity.class);
        startActivity(intent);
    }
}