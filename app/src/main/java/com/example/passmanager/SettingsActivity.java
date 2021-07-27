package com.example.passmanager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.example.passmanager.dialogs.MasterPasswordDialogFragment;
import com.example.passmanager.utils.CryptoHelper;

import java.util.Arrays;

/**
 * Activity for changing settings in the Password Manager.
 */
public class SettingsActivity extends AppCompatActivity
        implements MasterPasswordDialogFragment.DialogListener {

    private String encryptedMaster;

    private SettingsFragment settingsFragment;

    private SwitchPreferenceCompat switchAutoDecrypt;

    private boolean enteredMasterPassword = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        settingsFragment = new SettingsFragment();
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, settingsFragment)
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Get the encrypted master password.
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file),
                Context.MODE_PRIVATE);
        encryptedMaster = sharedPref.getString(getString(R.string.encrypted_master_key), null);
    }

    /**
     * Setup a listener for when the auto-decrypt option is changed.
     */
    public void setupAutoDecryptSettingListener() {
        switchAutoDecrypt = settingsFragment
                .findPreference(getString(R.string.entry_autodecrypt_setting));
        if (switchAutoDecrypt != null) {
            switchAutoDecrypt.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean autoDecrypt = (boolean) newValue;
                if (autoDecrypt && !enteredMasterPassword) {
                    // If the user wants to enable auto-decrypt, he must authenticate with the
                    // master password.
                    loadDialog();
                    return false;
                }
                return true;
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onClickPositiveButton(DialogInterface dialogInterface) {
        // Get the password entered by the user.
        AlertDialog dialog = (AlertDialog) dialogInterface;
        EditText passwordField = dialog.findViewById(R.id.editTextMasterPassword);

        Editable editable = passwordField.getText();
        char[] inputPass = new char[editable.length()];
        editable.getChars(0, editable.length(), inputPass, 0);

        // Check that it matches with the master password.
        char[] plaintextMaster = CryptoHelper.decryptMasterPassword(encryptedMaster);
        if (Arrays.equals(inputPass, plaintextMaster)) {
            // Authentication successful.
            Arrays.fill(inputPass, (char) 0);

            // Mark authentication completion.
            enteredMasterPassword = true;
            // Switch auto decrypt on after a successful authentication.
            switchAutoDecrypt.setChecked(true);

            dialog.dismiss();
        } else {
            // Authentication failed.
            Toast.makeText(getApplicationContext(), R.string.invalid_master_password,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Prompts the user for the master password in order to change a setting.
     */
    public void loadDialog() {
        DialogFragment dialogFragment = new MasterPasswordDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), "dialogMasterPass");
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            SettingsActivity settingsActivity = (SettingsActivity) getActivity();
            if (settingsActivity != null) { settingsActivity.setupAutoDecryptSettingListener();}
        }
    }
}