package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.passmanager.model.Entry;
import com.example.passmanager.utils.CryptoHelper;
import com.example.passmanager.viewmodel.ApplicationViewModel;

import java.util.Arrays;

import javax.crypto.SecretKey;

public class UpdateMasterPassActivity extends AppCompatActivity {

    private ApplicationViewModel viewmodel;

    private EditText currentMasterPassField;
    private EditText firstPassField;
    private EditText secondPassField;
    private TextView notMatchingTextView;

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
        notMatchingTextView = findViewById(R.id.notMatchingTextView);
        currentMasterPassField = findViewById(R.id.masterPassEditText);
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
            }
        });

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file),
                                                            Context.MODE_PRIVATE);
        encryptedMaster = sharedPref.getString(getString(R.string.encrypted_master_key), null);

        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);
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
        if (Arrays.equals(inputPass, plaintextMaster)
                && equalPasswords && passStrongness == CryptoHelper.PASS_MAX_STRONGNESS) {
            viewmodel.getAllEntries().observe(this, entries -> {

                // Update the entries by encrypting the passwords using the new master password.
                if (entries != null && canEnter) {
                    canEnter = false;
                    for (Entry entry : entries) {
                        byte[] oldSaltBytes = CryptoHelper.decode(entry.passwordSalt);
                        SecretKey oldKey = CryptoHelper.createPbeKey(plaintextMaster,
                                oldSaltBytes, false);
                        char[] decryptedUserPassword = CryptoHelper.decrypt(oldKey, entry.userPassword);

                        byte[] newSaltBytes = CryptoHelper.generateSalt();
                        SecretKey newKey = CryptoHelper.createPbeKey(newPass1, newSaltBytes, false);
                        String encryptedUserPassword = CryptoHelper.encrypt(newKey,
                                decryptedUserPassword, true);

                        entry.passwordSalt = CryptoHelper.encode(newSaltBytes);
                        entry.userPassword = encryptedUserPassword;
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

        } else {
            notMatchingTextView.setText(R.string.passwords_not_matching);
        }
    }
}