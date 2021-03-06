package com.example.passmanager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.Toast;

import com.example.passmanager.dialogs.LoadingDialogFragment;
import com.example.passmanager.model.ApplicationDatabase;
import com.example.passmanager.utils.CryptoHelper;
import com.example.passmanager.utils.DriveHelper;
import com.example.passmanager.utils.NetworkHelper;
import com.example.passmanager.viewmodel.ApplicationViewModel;
import com.google.android.gms.common.SignInButton;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Activity used for authenticating the user to the Password Manager.
 */
public class AuthActivity extends AppCompatActivity {

    private EditText masterPassField;
    private ApplicationViewModel viewmodel;

    private TextInputLayout masterPassLayout;

    SharedPreferences sharedPref;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long MINUTES_RESET_ATTEMPTS = 15;

    private int loginAttemptsLeft;
    private long lastAttempt;

    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private DriveHelper driveHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        masterPassField = findViewById(R.id.editTextPassword);
        masterPassLayout = findViewById(R.id.textLayout1);
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

        driveHelper = DriveHelper.getInstance();
        googleSignInLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                // Pass the sign in result to the DriveHelper.
                                driveHelper.onSignInResult(result.getData(), this);
                            }
                        });
        SignInButton syncDbBtn = findViewById(R.id.syncDbBtn);
        DriveHelper.setGoogleButtonText(syncDbBtn, getString(R.string.sync));
        syncDbBtn.setOnClickListener(view -> {
            if (NetworkHelper.isInternetConnectionAvailable(this)) {
                driveHelper.signIn(googleSignInLauncher, this);
                driveHelper.showDriveDbSyncDialog(this, (success -> {
                    // Upload finished.
                    if (success) {
                        Toast.makeText(this, R.string.upload_successful,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.upload_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                }), (success -> {
                    // Download finished.
                    if (success) {
                        Toast.makeText(this, R.string.download_successful,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.download_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                }));
            } else {
                Toast.makeText(this, R.string.no_internet_connection,  Toast.LENGTH_SHORT).show();
            }
        });
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

    public void resetSessionExpireTime() {
        sharedPref.edit()
                .putString(getString(R.string.session_expire_time_key), null)
                .apply();
    }

    /**
     * Called when a wrong master password is entered. Decrement the number of authentication
     * attempts left and update the time of the last attempt. If there are no more login attempts
     * remaining, delete the data repository with all the entries in the Password Manager.
     */
    private void failedAttempt() {
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
        masterPassLayout.setHelperTextEnabled(true);
        masterPassLayout.setHelperText(getString(R.string.invalid_master_password));
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

        // Display a loading dialog box.
        DialogFragment loadingDialog = new LoadingDialogFragment();
        loadingDialog.show(getSupportFragmentManager(), "dialogLoading");

        if (!viewmodel.open(getApplication(), pass, false)) {
            // Authentication failed.
            loadingDialog.dismiss();
            failedAttempt();
        } else {
            // Authentication successful.
            resetAttempts();
            resetSessionExpireTime();

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