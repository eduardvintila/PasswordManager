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
import android.widget.Toast;

import com.example.passmanager.model.ApplicationDatabase;
import com.example.passmanager.utils.CryptoHelper;
import com.example.passmanager.viewmodel.ApplicationViewModel;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Activity used for authenticating the user to the Password Manager.
 */
public class AuthActivity extends AppCompatActivity {

    private TextView validationMsg;
    private EditText masterPassField;
    private ApplicationViewModel viewmodel;

    SharedPreferences sharedPref;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long MINUTES_RESET_ATTEMPTS = 15;

    private int loginAttemptsLeft;
    private long lastAttempt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
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
        sharedPref = getSharedPreferences(getString(R.string.preference_file), Context.MODE_PRIVATE);
        extractAttempts();
    }

    /**
     * Extract the number of authentication attempts left and the time of last attempt from the
     * Shared Preferences.
     */
    private void extractAttempts() {
        lastAttempt = sharedPref.getLong(getString(R.string.last_attempt_key), 0);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(currentTime - lastAttempt);
        if (minutes > MINUTES_RESET_ATTEMPTS) {
            resetAttempts();
        } else {
            loginAttemptsLeft = sharedPref.getInt(getString(R.string.login_attempts_left_key),
                    MAX_LOGIN_ATTEMPTS);
        }
    }

    private void resetAttempts() {
        loginAttemptsLeft = MAX_LOGIN_ATTEMPTS;
        lastAttempt = 0;
    }

    /**
     * Called when a wrong master password is entered. Decrement the number of authentication
     * attempts left and update the time of the last attempt. If there are no more login attempts
     * remaining, delete the data repository with all the entries in the Password Manager.
     */
    private void failedAttempt() {
        validationMsg.setText(R.string.invalid_pass);
        lastAttempt = Calendar.getInstance().getTimeInMillis();
        loginAttemptsLeft--;
        if (loginAttemptsLeft == 0 ) {
            // No more attempts left. Delete the database.
            viewmodel.delete(getApplication());
            resetAttempts();
            Toast.makeText(this, R.string.database_deleted, Toast.LENGTH_LONG).show();
            goToCreate();
        } else {
            Toast.makeText(this, getString(R.string.attempts_left) + loginAttemptsLeft,
                    Toast.LENGTH_SHORT).show();
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

        if (!viewmodel.open(getApplication(), pass, false)) {
            // Authentication failed.
            failedAttempt();
        } else {
            // Authentication successful.
            resetAttempts();

            // Encrypt the master password and pass it to the next activities in order to use it
            // for encrypting/decrypting passwords in the entries.
            String encrypted = CryptoHelper.encryptMasterPassword(pass, true);

            // Use SharedPreferences to pass the encrypted master password to other activities.
            sharedPref.edit()
                    .putString(getString(R.string.encrypted_master_key), encrypted)
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

    @Override
    protected void onPause() {
        super.onPause();
        // Save the number of attempts left and the time of last attempt to the Shared Preferences.
        sharedPref.edit()
                .putInt(getString(R.string.login_attempts_left_key), loginAttemptsLeft)
                .putLong(getString(R.string.last_attempt_key), lastAttempt)
                .apply();
    }
}