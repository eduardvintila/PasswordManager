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

public class CreateEntryActivity extends AppCompatActivity {

    private EditText entryNameField;
    private EditText userIdField;
    private EditText userPasswordField;
    private EditText entryDescriptionField;
    private EditText serviceLinkField;
    private EntryViewModel entryVm;
    private TextInputLayout userPasswordLayout;
    // TODO: Implement Picture path field

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_entry);

        entryNameField = findViewById(R.id.entryNameEditText);
        userIdField = findViewById(R.id.userIdEditText);
        userPasswordField = findViewById(R.id.userPassEditText);
        userPasswordLayout = findViewById(R.id.userPassTextLayout);
        entryDescriptionField = findViewById(R.id.entryDescriptionEditText);
        serviceLinkField = findViewById(R.id.serviceLinkEditText);

        entryVm = new ViewModelProvider(this).get(EntryViewModel.class);
    }

    public void createEntry(View view) {
        // TODO: Validations

        Intent prevIntent = getIntent();
        String encryptedMaster = prevIntent.getStringExtra(MainActivity.EXTRA_ENCRYPTED_MASTER);
        String plainTextMaster = CryptoHelper.decryptMasterPassword(encryptedMaster);

        // TODO: Maybe wrap these lines in a function in CryptoHelper?
        String userPassword = userPasswordField.getText().toString();
        byte[] saltBytes = CryptoHelper.generateSalt();
        SecretKey key = CryptoHelper.createPbeKey(plainTextMaster, saltBytes);
        String encryptedUserPassword = CryptoHelper.encrypt(key, userPassword);


        Entry entry = new Entry(entryNameField.getText().toString(),
                entryDescriptionField.getText().toString(), null,
                serviceLinkField.getText().toString(), userIdField.getText().toString(),
                encryptedUserPassword, CryptoHelper.bytesToHexString(saltBytes));

        entryVm.insert(entry);
        finish();

    }


    public void generatePassword(View view) {
        String pass = CryptoHelper.generatePassword(16);
        userPasswordField.setText(pass);
    }
}