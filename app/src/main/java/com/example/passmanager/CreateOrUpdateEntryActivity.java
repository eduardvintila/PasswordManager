package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.sql.Date;

import javax.crypto.SecretKey;

/**
 * Activity for creating a new entry or modifying an existing one.
 */
public class CreateOrUpdateEntryActivity extends AppCompatActivity {

    // TODO: Implement Picture path field
    private EditText entryNameField;
    private EditText userIdField;
    private EditText userPasswordField;
    private EditText entryDescriptionField;
    private EditText serviceLinkField;
    private TextView passNotStrongTextView;
    private EntryViewModel entryVm;

    // The encrypted master password used in encrypting the password in the entry.
    private String encryptedMaster;

    // Only used if updating an existing entry.
    private Entry oldEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_update_entry);

        entryNameField = findViewById(R.id.entryNameEditText);
        userIdField = findViewById(R.id.userIdEditText);
        userPasswordField = findViewById(R.id.userPassEditText);
        entryDescriptionField = findViewById(R.id.entryDescriptionEditText);
        serviceLinkField = findViewById(R.id.serviceLinkEditText);
        passNotStrongTextView = findViewById(R.id.passNotStrongTextView);

        entryVm = new ViewModelProvider(this).get(EntryViewModel.class);

        Intent prevIntent = getIntent();
        encryptedMaster = prevIntent.getStringExtra(MainActivity.EXTRA_ENCRYPTED_MASTER);
        // Check whether we are updating an existing entry or creating a new one
        if (prevIntent.hasExtra(EntriesMenuActivity.EXTRA_ENTRY_ID)) {
            int entryId = prevIntent.getIntExtra(EntriesMenuActivity.EXTRA_ENTRY_ID, 1);
            entryVm.getEntry(entryId).observe(this, entry -> {
                if (oldEntry == null && entry != null) {
                    oldEntry = entry;
                    // When updating an existing entry, load all text fields with the old
                    // information.
                    entryNameField.setText(entry.entryName);
                    userIdField.setText(entry.userId);
                    entryDescriptionField.setText(entry.entryDescription);
                    serviceLinkField.setText(entry.serviceLink);

                    // Also populate the password field with the plaintext password decrypted
                    // previously.
                    String pass = prevIntent.getStringExtra(EntryActivity.EXTRA_ENTRY_PASSWORD);
                    userPasswordField.setText(pass);
                }
            });
        }

        userPasswordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String pass = s.toString();
                if (CryptoHelper.passwordStrongness(pass) < CryptoHelper.PASS_MAX_STRONGNESS) {
                    passNotStrongTextView.setVisibility(View.VISIBLE);
                } else {
                    passNotStrongTextView.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    /**
     * Save an entry. Either modify an existing one or create a new one.
     *
     * @param view The clicked button.
     */
    public void saveEntry(View view) {
        char[] plainTextMaster = CryptoHelper.decryptMasterPassword(encryptedMaster);

        // Use the master password and a random salt to encrypt the password for the entry.
        Editable editable = userPasswordField.getText();
        char[] userPassword = new char[editable.length()];
        editable.getChars(0, editable.length(), userPassword, 0);

        // String userPassword = userPasswordField.getText().toString();
        byte[] saltBytes = CryptoHelper.generateSalt();
        SecretKey key = CryptoHelper.createPbeKey(plainTextMaster, saltBytes);
        String encryptedUserPassword = CryptoHelper.encrypt(key, userPassword);

        if (oldEntry != null) {
            // Update an existing entry.
            Entry newEntry = oldEntry;
            newEntry.entryName = entryNameField.getText().toString();
            newEntry.entryDescription = entryDescriptionField.getText().toString();
            newEntry.serviceLink = serviceLinkField.getText().toString();
            newEntry.userId = userIdField.getText().toString();
            newEntry.userPassword = encryptedUserPassword;
            newEntry.passwordSalt = CryptoHelper.encode(saltBytes);
            newEntry.lastModified = new Date(Calendar.getInstance().getTimeInMillis());

            entryVm.updateEntry(newEntry);
        } else {
            // Create a new entry.
            Entry entry = new Entry(entryNameField.getText().toString(),
                    entryDescriptionField.getText().toString(), null,
                    serviceLinkField.getText().toString(), userIdField.getText().toString(),
                    encryptedUserPassword, CryptoHelper.encode(saltBytes),
                    new Date(Calendar.getInstance().getTimeInMillis()));

            entryVm.insert(entry);
        }

        finish();
    }


    /**
     * Generate a random password for the entry.
     * TODO: Add options for custom password length, characters etc.
     *
     * @param view The clicked button.
     */
    public void generatePassword(View view) {
        String pass = CryptoHelper.generatePassword(16);
        userPasswordField.setText(pass);
    }
}