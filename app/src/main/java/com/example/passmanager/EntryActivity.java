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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.passmanager.dialogs.MasterPasswordDialogFragment;
import com.example.passmanager.model.Entry;
import com.example.passmanager.utils.CryptoHelper;
import com.example.passmanager.viewmodel.ApplicationViewModel;

import java.util.Arrays;

import javax.crypto.SecretKey;

/**
 * Activity used for visualising a particular entry in the Password Manager.
 */
public class EntryActivity extends AppCompatActivity
        implements MasterPasswordDialogFragment.DialogListener {

    // Identifier for passing the entry password in an intent to another activity.
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

    private ApplicationViewModel viewmodel;

    private String encryptedMaster;

    private Entry entry;

    private boolean userPasswordDecrypted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);

        entryNameField = findViewById(R.id.entryNameTextView);
        userIdField = findViewById(R.id.userIdTextView);
        userPasswordField = findViewById(R.id.userPassTextView);
        entryDescriptionField = findViewById(R.id.entryDescriptionTextView);
        serviceLinkField = findViewById(R.id.serviceLinkTextView);
        entryLastModifiedField = findViewById(R.id.lastModifiedTextView);
        decryptBtn = findViewById(R.id.loadDialogBtn);
        copyPassBtn = findViewById(R.id.copyPassBtn);
        copyUserBtn = findViewById(R.id.copyUserBtn);

        decryptBtn.setOnClickListener(view -> loadDialog());
        copyPassBtn.setOnClickListener(view -> copyPass());
        copyUserBtn.setOnClickListener(view -> copyUser());
        findViewById(R.id.modifyBtn).setOnClickListener(view -> modifyEntry());
        findViewById(R.id.deleteBtn).setOnClickListener(view -> deleteEntry());

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file),
                                                            Context.MODE_PRIVATE);
        encryptedMaster = sharedPref.getString(getString(R.string.encrypted_master), null);

        Intent intent = getIntent();
        if (intent != null) {
            int entryId = intent.getIntExtra(EntriesMenuActivity.EXTRA_ENTRY_ID, 1);
            viewmodel.getEntry(entryId).observe(this, entry -> {
                if (entry != null) {
                    // Populate the fields with the entry information.
                    this.entry = entry;
                    entryNameField.setText(entry.entryName);
                    userIdField.setText(entry.userId);
                    userPasswordField.setText(entry.userPassword);
                    entryDescriptionField.setText(entry.entryDescription);
                    serviceLinkField.setText(entry.serviceLink);
                    entryLastModifiedField.setText(entry.lastModified.toString());

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
     */
    public void loadDialog() {
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
            SecretKey key = CryptoHelper.createPbeKey(plaintextMaster, saltBytes, true);
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
     */
    public void deleteEntry() {
        // TODO: Delete entry only if master password is provided?
        viewmodel.deleteEntry(entry);
        entry = null;
        Toast.makeText(this, R.string.entry_deleted, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Modify the entry in the repository.
     */
    public void modifyEntry() {
        if (userPasswordDecrypted) {
            // Load the activity where the user can edit the entry information.
            Intent intent = new Intent(this, CreateOrUpdateEntryActivity.class);
            intent.putExtra(EntriesMenuActivity.EXTRA_ENTRY_ID, entry.entryNo);
            intent.putExtra(EntryActivity.EXTRA_ENTRY_PASSWORD, userPasswordField.getText().toString());
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.decrypt_password_first, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Copy the password in the entry to clipboard.
     */
    public void copyPass() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.label_user_password),
                userPasswordField.getText().toString());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, R.string.password_copied, Toast.LENGTH_SHORT).show();
    }

    /**
     * Copy user id to clipboard.
     */
    public void copyUser() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("user ID",
                userIdField.getText().toString());
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, R.string.user_copied, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // When exiting the view entry menu, clear the password from the clipboard.
        // Works good on a real device, doesn't seem to work with the emulator.
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String passLabel = getString(R.string.label_user_password);
        if (clipboard.getPrimaryClipDescription().getLabel().equals(passLabel)) {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
        }
    }
}