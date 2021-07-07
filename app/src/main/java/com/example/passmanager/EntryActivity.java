package com.example.passmanager;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import javax.crypto.SecretKey;

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
                this.entry = entry; // Save the entry information.
                entryNameField.setText(entry.entryName);
                userIdField.setText(entry.userId);
                userPasswordField.setText(entry.userPassword);
                entryDescriptionField.setText(entry.entryDescription);
                serviceLinkField.setText(entry.serviceLink);
                encryptedMaster = intent.getStringExtra(MainActivity.EXTRA_ENCRYPTED_MASTER);
            });
        } else {
            // Kill the activity if it hasn't received an entry.
            finish();
        }

    }

    public void loadDialog(View view) {
        // Check if the entry has been loaded.
        if (entry != null) {
            DialogFragment dialogFragment = new MasterPasswordDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "dialogMasterPass");
        }
    }

    @Override
    public void onClickPositiveButton(DialogInterface dialogInterface) {
        AlertDialog dialog = (AlertDialog) dialogInterface;
        EditText passwordField = dialog.findViewById(R.id.editTextMasterPassword);
        String inputPass = passwordField.getText().toString();
        String plaintextMaster = CryptoHelper.decryptMasterPassword(encryptedMaster);
        if (inputPass.equals(plaintextMaster)) {
            String encryptedUserPassword = entry.userPassword;
            byte[] saltBytes = CryptoHelper.hexStringToBytes(entry.passwordSalt);
            SecretKey key = CryptoHelper.createPbeKey(plaintextMaster, saltBytes);
            String decryptedUserPassword = CryptoHelper.decrypt(key, encryptedUserPassword);
            userPasswordField.setText(decryptedUserPassword);
            findViewById(R.id.imageButton).setVisibility(View.INVISIBLE);
            Toast.makeText(getApplicationContext(), R.string.userPassDecryptedMsg,
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        } else {
            // TODO: Show a message that the password is invalid.
            Toast.makeText(getApplicationContext(), R.string.masterPassInvalidMsg,
                    Toast.LENGTH_SHORT).show();
        }
    }
}