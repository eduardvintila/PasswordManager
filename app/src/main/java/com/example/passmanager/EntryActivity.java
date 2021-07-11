package com.example.passmanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import javax.crypto.SecretKey;

/**
 * Activity used for visualising a particular entry in the Password Manager.
 */
public class EntryActivity extends AppCompatActivity
        implements MasterPasswordDialogFragment.DialogListener {

    public static final String EXTRA_ENTRY_PASSWORD = BuildConfig.APPLICATION_ID +
            ".ENTRY_PASSWORD";

    private TextView entryNameField;
    private TextView userIdField;
    private TextView userPasswordField;
    private TextView entryDescriptionField;
    private TextView serviceLinkField;
    private TextView entryLastModifiedField;
    private Button decryptBtn;
    private ImageButton copyPassBtn;
    private ImageButton copyUserBtn;

    private EntryViewModel entryVm;

    private String encryptedMaster;

    private Entry entry;

    private boolean userPasswordDecrypted = false;

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
        entryLastModifiedField = findViewById(R.id.lastModifiedTextView);
        decryptBtn = findViewById(R.id.loadDialogBtn);
        copyPassBtn = findViewById(R.id.copyPassBtn);
        copyUserBtn = findViewById(R.id.copyUserBtn);

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
                    entryLastModifiedField.setText(entry.lastModified.toString());
                    encryptedMaster = intent.getStringExtra(MainActivity.EXTRA_ENCRYPTED_MASTER);

                    decryptBtn.setVisibility(View.VISIBLE);
                    userPasswordDecrypted = false;
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

        // Get the password from the input field.
        Editable editable = passwordField.getText();
        char[] inputPass = new char[editable.length()];
        editable.getChars(0, editable.length(), inputPass, 0);

        // Check that it matches with the master password.
        char[] plaintextMaster = CryptoHelper.decryptMasterPassword(encryptedMaster);
        if (Arrays.equals(inputPass, plaintextMaster)) {
            Arrays.fill(inputPass, (char) 0);

            // Decrypt the password in the entry.
            String encryptedUserPassword = entry.userPassword;
            byte[] saltBytes = CryptoHelper.decode(entry.passwordSalt);
            SecretKey key = CryptoHelper.createPbeKey(plaintextMaster, saltBytes);
            char[] decryptedUserPassword = CryptoHelper.decrypt(key, encryptedUserPassword);

            if (decryptedUserPassword != null){
                userPasswordField.setText(decryptedUserPassword, 0, decryptedUserPassword.length);
            }
            // Since the password has been decrypted, hide the "decrypt password" button.
            decryptBtn.setVisibility(View.INVISIBLE);
            userPasswordDecrypted = true;

            // And make the copy password button visible.
            copyPassBtn.setVisibility(View.VISIBLE);

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
        // TODO: Delete entry only if master password is provided?
        entryVm.deleteEntry(entry);
        entry = null;
        Toast.makeText(this, R.string.entry_deleted, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Modify the entry in the repository.
     *
     * @param view The clicked button.
     */
    public void modifyEntry(View view) {
        /*// Load the dialog which prompts the user for the master password.
        loadDialog(view);*/
        if (userPasswordDecrypted) {
            // Load the activity where the user can edit the entry information.
            Intent intent = new Intent(this, CreateOrUpdateEntryActivity.class);
            intent.putExtra(EntriesMenuActivity.EXTRA_ENTRY_ID, entry.entryNo);
            intent.putExtra(EntryActivity.EXTRA_ENTRY_PASSWORD, userPasswordField.getText().toString());
            intent.putExtra(MainActivity.EXTRA_ENCRYPTED_MASTER, encryptedMaster);
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.decrypt_password_first, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Copy the password in the entry to clipboard.
     */
    public void copyPass(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("user Password",
                userPasswordField.getText().toString());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, R.string.password_copied, Toast.LENGTH_SHORT).show();
    }

    /**
     * Copy user id to clipboard.
     */
    public void copyUser(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("user ID",
                userIdField.getText().toString());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, R.string.user_copied, Toast.LENGTH_SHORT).show();
    }
}