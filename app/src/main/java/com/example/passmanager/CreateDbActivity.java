package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import com.example.passmanager.utils.CryptoHelper;
import com.example.passmanager.viewmodel.ApplicationViewModel;

import java.util.Arrays;

/**
 * Activity for creating a new database of entries.
 */
public class CreateDbActivity extends AppCompatActivity {

    private ApplicationViewModel viewmodel;
    private EditText firstPassField;
    private EditText secondPassField;
    private TextView notMatchingTextView;

    char[] pass1;

    private int passStrongness = 0;
    private boolean equalPasswords = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_db);
        firstPassField = findViewById(R.id.firstPassEditText);
        secondPassField = findViewById(R.id.secondPassEditText);
        notMatchingTextView = findViewById(R.id.notMatchingTextView);
        findViewById(R.id.updateMasterPassBtn).setOnClickListener(view -> create());

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
    }

    /**
     * Create a new database.
     */
    public void create() {
        if (equalPasswords && passStrongness == CryptoHelper.PASS_MAX_STRONGNESS) {
            viewmodel.create(getApplication(), pass1, true);
            viewmodel.close();
            finish();
        } else {
            // Passwords not matching or not strong enough.
            notMatchingTextView.setText(R.string.passwords_not_matching);
        }
    }
}