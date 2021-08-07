package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.passmanager.dialogs.LoadingDialogFragment;
import com.example.passmanager.model.Entry;
import com.example.passmanager.utils.CryptoHelper;
import com.example.passmanager.viewmodel.ApplicationViewModel;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;

import javax.crypto.SecretKey;

public class UpdateMasterPassActivity extends AppCompatActivity {

    private ApplicationViewModel viewmodel;

    private EditText currentMasterPassField;
    private EditText firstPassField;
    private EditText secondPassField;

    private TextInputLayout currentMasterPassLayout;
    private TextInputLayout firstPassLayout;
    private TextInputLayout secondPassLayout;

    char[] newPass1;

    private int passStrongness = 0;
    private boolean equalPasswords = false;
    private String encryptedMaster;

    // Used for making sure that the observer in the update master pass method executes only once.
    private boolean canEnter = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_master_pass);
        firstPassField = findViewById(R.id.firstPassEditText);
        secondPassField = findViewById(R.id.secondPassEditText);
        currentMasterPassField = findViewById(R.id.masterPassEditText);
        currentMasterPassLayout = findViewById(R.id.textLayout1);
        firstPassLayout = findViewById(R.id.textLayout2);
        secondPassLayout = findViewById(R.id.textLayout3);
        findViewById(R.id.updateMasterPassBtn).setOnClickListener(view -> updateMasterPass());

        firstPassField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable e1) {
                Editable e2 = secondPassField.getText();
                if (newPass1 != null) { Arrays.fill(newPass1, (char) 0); } // Clear the previous password.
                newPass1 = new char[e1.length()];
                char[] newPass2 = new char[e2.length()];

                e1.getChars(0, e1.length(), newPass1, 0);
                e2.getChars(0, e2.length(), newPass2, 0);
                passStrongness = CryptoHelper.passwordStrongness(newPass1);
                equalPasswords = Arrays.equals(newPass1, newPass2);
                Arrays.fill(newPass2, (char) 0);

                validate();
            }
        });

        secondPassField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable e2) {
                char[] newPass2 = new char[e2.length()];

                e2.getChars(0, e2.length(), newPass2, 0);
                equalPasswords = Arrays.equals(newPass1, newPass2);
                Arrays.fill(newPass2, (char) 0);

                validate();
            }
        });

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file),
                                                            Context.MODE_PRIVATE);
        encryptedMaster = sharedPref.getString(getString(R.string.encrypted_master_key), null);

        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);
    }

    /**
     * Validate the new password fields.
     *
     * @return <code>true</code> if the fields are valid; <code>false</code> otherwise.
     */
    private boolean validate() {
        boolean valid = true;

        if (passStrongness < CryptoHelper.PASS_MAX_STRONGNESS) {
            firstPassLayout.setHelperTextEnabled(true);
            firstPassLayout.setHelperText(getString(R.string.password_not_strong));
            valid = false;
        } else {
            firstPassLayout.setHelperTextEnabled(false);
        }

        if (!equalPasswords) {
            if (secondPassField.length() > 0) {
                secondPassLayout.setHelperTextEnabled(true);
                secondPassLayout.setHelperText(getString(R.string.passwords_not_matching));
            }
            valid = false;
        } else {
            secondPassLayout.setHelperTextEnabled(false);
        }

        return valid;
    }

    /**
     * Update the master password.
     */
    public void updateMasterPass() {
        // Get the current master password from the input field.
        Editable editable = currentMasterPassField.getText();
        char[] inputPass = new char[editable.length()];
        editable.getChars(0, editable.length(), inputPass, 0);


        // Check that it matches with the master password.
        char[] plaintextMaster = CryptoHelper.decryptMasterPassword(encryptedMaster);
        boolean isMasterPassCorrect = Arrays.equals(inputPass, plaintextMaster);
        if (isMasterPassCorrect && validate()) {
            viewmodel.getAllEntries().observe(this, entries -> {
                // Display a loading dialog box.
                DialogFragment loadingDialog = new LoadingDialogFragment();
                loadingDialog.show(getSupportFragmentManager(), "dialogLoading");

                // Update the entries by encrypting the passwords using the new master password.
                if (entries != null && canEnter) {
                    canEnter = false;
                    for (Entry entry : entries) {
                        byte[] oldSaltBytes = CryptoHelper.decode(entry.passwordSalt);
                        SecretKey oldKey = CryptoHelper.createPbeKey(plaintextMaster,
                                oldSaltBytes, false);
                        char[] decryptedEntryPassword = CryptoHelper.decrypt(oldKey, entry.password);

                        byte[] newSaltBytes = CryptoHelper.generateSalt();
                        SecretKey newKey = CryptoHelper.createPbeKey(newPass1, newSaltBytes, false);
                        String encryptedEntryPassword = CryptoHelper.encrypt(newKey,
                                decryptedEntryPassword, true);

                        entry.passwordSalt = CryptoHelper.encode(newSaltBytes);
                        entry.password = encryptedEntryPassword;
                    }
                    // Clear the old master password.
                    Arrays.fill(plaintextMaster, (char) 0);

                    viewmodel.updateEntries(entries);
                    viewmodel.changeMasterPassword(newPass1);

                    String newPassEncrypted = CryptoHelper.encryptMasterPassword(newPass1, true);

                    // Save the new encrypted master password.
                    SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file),
                                                                        Context.MODE_PRIVATE);
                    sharedPref.edit()
                            .putString(getString(R.string.encrypted_master_key), newPassEncrypted)
                            .apply();

                    Arrays.fill(newPass1, (char) 0);
                    Toast.makeText(this, R.string.master_pass_modified, Toast.LENGTH_SHORT)
                            .show();
                    finish();
                }
            });
        } else if (!isMasterPassCorrect) {
            currentMasterPassLayout.setHelperTextEnabled(true);
            currentMasterPassLayout.setHelperText(getString(R.string.invalid_master_password));
        } else if (isMasterPassCorrect) {
            currentMasterPassLayout.setHelperTextEnabled(false);
        }
    }
}