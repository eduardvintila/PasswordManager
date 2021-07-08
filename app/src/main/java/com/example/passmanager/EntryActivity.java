package com.example.passmanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import javax.crypto.SecretKey;

/**
 * Activity used for visualising a particular entry in the Password Manager.
 */
public class EntryActivity extends AppCompatActivity
        implements MasterPasswordDialogFragment.DialogListener {

    private TextView entryNameField;
    private TextView userIdField;
    private TextView userPasswordField;
    private TextView entryDescriptionField;
    private TextView serviceLinkField;
    private EntryViewModel entryVm;

    private String encryptedMaster;

    private Entry entry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        entryVm = new ViewModelProvider(this).get(EntryViewModel.class);

        entryNameField = findViewById(R.id.entryNameTextView);
        userIdField = findViewById(R.id.userIdTextView);
        userPasswordField = findViewById(R.id.userPassTextView);
        entryDescriptionField = findViewById(R.id.entryDescriptionTextView);
        serviceLinkField = findViewById(R.id.serviceLinkTextView);

        Intent intent = getIntent();
        if (intent != null) {
            int entryId = intent.getIntExtra(EntriesMenuActivity.EXTRA_ENTRY_ID, 1);
            entryVm.getEntry(entryId).observe(this, entry -> {
                if (entry != null) {
                    // Save the entry information.
                    this.entry = entry;
                    entryNameField.setText(entry.entryName);
                    userIdField.setText(entry.userId);
                    userPasswordField.setText(entry.userPassword);
                    entryDescriptionField.setText(entry.entryDescription);
                    serviceLinkField.setText(entry.serviceLink);
                    encryptedMaster = intent.getStringExtra(MainActivity.EXTRA_ENCRYPTED_MASTER);
                }
            });
        } else {
            // Kill the activity if it hasn't received an entry.
            finish();
        }

    }

    /**
     * Load the dialog box which prompts the user to enter the master password in order to
     * decrypt the password stored in the entry.
     *
     * TODO: Make this optional.
     *
     * @param view The button pressed.
     */
    public void loadDialog(View view) {
        // Check if the entry has been loaded.
        if (entry != null) {
            DialogFragment dialogFragment = new MasterPasswordDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "dialogMasterPass");
        }
    }

    /**
     * Called when the password in the dialog box has been entered by the user.
     *
     * @param dialogInterface The dialog box.
     */
    @Override
    public void onClickPositiveButton(DialogInterface dialogInterface) {
        // Get the password entered by the user.
        AlertDialog dialog = (AlertDialog) dialogInterface;
        EditText passwordField = dialog.findViewById(R.id.editTextMasterPassword);
        String inputPass = passwordField.getText().toString();

        // Check that it matches with the master password.
        String plaintextMaster = CryptoHelper.decryptMasterPassword(encryptedMaster);
        if (inputPass.equals(plaintextMaster)) {
            // Decrypt the password in the entry.
            String encryptedUserPassword = entry.userPassword;
            byte[] saltBytes = CryptoHelper.hexStringToBytes(entry.passwordSalt);
            SecretKey key = CryptoHelper.createPbeKey(plaintextMaster, saltBytes);
            String decryptedUserPassword = CryptoHelper.decrypt(key, encryptedUserPassword);

            userPasswordField.setText(decryptedUserPassword);
            // Since the password has been decrypted, hide the "decrypt password" button.
            findViewById(R.id.loadDialogBtn).setVisibility(View.INVISIBLE);

            Toast.makeText(getApplicationContext(), R.string.user_pass_decrypted,
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        } else {
            Toast.makeText(getApplicationContext(), R.string.invalid_master_password,
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Delete the entry from the repository.
     *
     * @param view The clicked button.
     */
    public void deleteEntry(View view) {
        entryVm.deleteEntry(entry);
        entry = null;
        Toast.makeText(this, R.string.entry_deleted, Toast.LENGTH_SHORT).show();
        finish();
    }
}