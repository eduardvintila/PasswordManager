package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Arrays;

/**
 * Activity for creating a new database of entries.
 */
public class CreateActivity extends AppCompatActivity {

    private EntryViewModel entryVm;
    private EditText firstPassField;
    private EditText secondPassField;
    private TextView notMatchingTextView;

    private int passStrongness = 0;
    private boolean equalPasswords = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);
        firstPassField = findViewById(R.id.firstPassEditText);
        secondPassField = findViewById(R.id.secondPassEditText);
        notMatchingTextView = findViewById(R.id.notMatchingTextView);
        entryVm = new ViewModelProvider(this).get(EntryViewModel.class);

        firstPassField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String pass1 = s.toString();
                String pass2 = secondPassField.getText().toString();
                passStrongness = CryptoHelper.passwordStrongness(pass1);
                equalPasswords = pass1.equals(pass2);
            }
        });

        secondPassField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String pass1 = firstPassField.getText().toString();
                String pass2 = s.toString();
                equalPasswords = pass1.equals(pass2);
            }
        });
    }

    public void goToCreate(View view) {
        char[] pass1 = firstPassField.getText().toString().toCharArray();
        char[] pass2 = secondPassField.getText().toString().toCharArray();

        if (equalPasswords && passStrongness == 4) {
            entryVm.create(getApplication(), pass1);
            entryVm.close();
            finish();
        } else {
            // Passwords not matching or not strong enough.
            notMatchingTextView.setText(R.string.passwords_not_matching);
        }
    }
}