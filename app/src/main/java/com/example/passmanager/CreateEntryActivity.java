package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import javax.crypto.SecretKey;

/**
 * Activity for creating a new entry.
 */
public class CreateEntryActivity extends AppCompatActivity {

    private EditText entryNameField;
    private EditText userIdField;
    private EditText userPasswordField;
    private EditText entryDescriptionField;
    private EditText serviceLinkField;
    private EntryViewModel entryVm;
    // TODO: Implement Picture path field

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_entry);

        entryNameField = findViewById(R.id.entryNameEditText);
        userIdField = findViewById(R.id.userIdEditText);
        userPasswordField = findViewById(R.id.userPassEditText);
        entryDescriptionField = findViewById(R.id.entryDescriptionEditText);
        serviceLinkField = findViewById(R.id.serviceLinkEditText);

        entryVm = new ViewModelProvider(this).get(EntryViewModel.class);
    }

    /**
     * Create a new entry.
     *
     * @param view The clicked button.
     */
    public void createEntry(View view) {
        // TODO: Validations

        // Get the encrypted master password passed by the previous activity.
        Intent prevIntent = getIntent();
        String encryptedMaster = prevIntent.getStringExtra(MainActivity.EXTRA_ENCRYPTED_MASTER);
        String plainTextMaster = CryptoHelper.decryptMasterPassword(encryptedMaster);

        // TODO: Maybe wrap these lines in a function in CryptoHelper?
        // Use the master password and a random salt to encrypt the password for the entry.
        String userPassword = userPasswordField.getText().toString();
        byte[] saltBytes = CryptoHelper.generateSalt();
        SecretKey key = CryptoHelper.createPbeKey(plainTextMaster, saltBytes);
        String encryptedUserPassword = CryptoHelper.encrypt(key, userPassword);


        Entry entry = new Entry(entryNameField.getText().toString(),
                entryDescriptionField.getText().toString(), null,
                serviceLinkField.getText().toString(), userIdField.getText().toString(),
                encryptedUserPassword, CryptoHelper.bytesToHexString(saltBytes));

        // Insert the new entry into the database and return to the entries menu.
        entryVm.insert(entry);
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