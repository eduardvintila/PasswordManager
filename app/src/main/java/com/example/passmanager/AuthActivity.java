package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.TextView;

import com.example.passmanager.model.ApplicationDatabase;
import com.example.passmanager.utils.CryptoHelper;
import com.example.passmanager.viewmodel.ApplicationViewModel;

import java.io.File;

/**
 * Activity used for authenticating the user to the Password Manager.
 */
public class AuthActivity extends AppCompatActivity {

    private TextView validationMsg;
    private EditText masterPassField;
    private ApplicationViewModel viewmodel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        validationMsg = findViewById(R.id.validationTextView);
        masterPassField = findViewById(R.id.editTextPassword);
        findViewById(R.id.authBtn).setOnClickListener(view -> auth());
        findViewById(R.id.goToCreateBtn).setOnClickListener(view -> goToCreate());

        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);

        File databaseFile = getDatabasePath(ApplicationDatabase.DB_NAME);
        if (!databaseFile.exists()) {
            // If a database doesn't exist, go to the creation menu.
            goToCreate();
        }
    }

    /**
     * Authenticate using the master password.
     */
    public void auth() {
        // Get the password from the input field.
        Editable editable = masterPassField.getText();
        char[] pass = new char[editable.length()];
        editable.getChars(0, editable.length(), pass, 0);

        if (pass.length == 0)
            return;

        viewmodel.open(getApplication(), pass, false);
        if (!viewmodel.isValidMasterPass()) {
            validationMsg.setText(R.string.invalid_pass);
        } else {
            // Encrypt the master password and pass it to the next activities in order to use it
            // for encrypting/decrypting passwords in the entries.
            String encrypted = CryptoHelper.encryptMasterPassword(pass);

            // Use SharedPreferences to pass the encrypted master password to other activities.
            SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file),
                                                                Context.MODE_PRIVATE);
            sharedPref.edit()
                    .putString(getString(R.string.encrypted_master), encrypted)
                    .apply();

            Intent intent = new Intent(this, EntriesMenuActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Switch to the create database menu.
     */
    public void goToCreate() {
        Intent intent = new Intent(this, CreateDbActivity.class);
        startActivity(intent);
    }
}