package com.example.passmanager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.passmanager.dialogs.LoadingDialogFragment;
import com.example.passmanager.utils.CryptoHelper;
import com.example.passmanager.utils.DriveHelper;
import com.example.passmanager.utils.NetworkHelper;
import com.example.passmanager.viewmodel.ApplicationViewModel;
import com.google.android.gms.common.SignInButton;

import java.util.Arrays;

/**
 * Activity for creating a new database of entries.
 */
public class CreateDbActivity extends AppCompatActivity {

    private ApplicationViewModel viewmodel;
    private EditText firstPassField;
    private EditText secondPassField;
    private TextView notMatchingTextView;

    // Password characters from the first field.
    char[] pass1;

    private int passStrongness = 0;
    private boolean equalPasswords = false;

    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private DriveHelper driveHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_db);
        firstPassField = findViewById(R.id.firstPassEditText);
        secondPassField = findViewById(R.id.secondPassEditText);
        notMatchingTextView = findViewById(R.id.notMatchingTextView);
        findViewById(R.id.createDbBtn).setOnClickListener(view -> create());

        firstPassField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable e1) {
                Editable e2 = secondPassField.getText();
                if (pass1 != null) { Arrays.fill(pass1, (char) 0); } // Clear the previous password.
                pass1 = new char[e1.length()];
                char[] pass2 = new char[e2.length()];

                e1.getChars(0, e1.length(), pass1, 0);
                e2.getChars(0, e2.length(), pass2, 0);
                passStrongness = CryptoHelper.passwordStrongness(pass1);
                equalPasswords = Arrays.equals(pass1, pass2);
                Arrays.fill(pass2, (char) 0);
            }
        });

        secondPassField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable e2) {
                char[] pass2 = new char[e2.length()];

                e2.getChars(0, e2.length(), pass2, 0);
                equalPasswords = Arrays.equals(pass1, pass2);
                Arrays.fill(pass2, (char) 0);
            }
        });

        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);

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
                            }  else {
                                Toast.makeText(this, R.string.upload_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }), (success -> {
                            // Download finished.
                            if (success) {
                                Toast.makeText(this, R.string.download_successful,
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, R.string.download_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }));
                    } else {
                        Toast.makeText(this, R.string.no_internet_connection,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Create a new database.
     */
    public void create() {
        if (equalPasswords && passStrongness == CryptoHelper.PASS_MAX_STRONGNESS) {
            // Display a loading dialog box.
            DialogFragment loadingDialog = new LoadingDialogFragment();
            loadingDialog.show(getSupportFragmentManager(), "dialogLoading");

            viewmodel.create(getApplication(), pass1, true);
            viewmodel.close();
            finish();
        } else {
            // Passwords not matching or not strong enough.
            notMatchingTextView.setText(R.string.passwords_not_matching);
        }
    }
}