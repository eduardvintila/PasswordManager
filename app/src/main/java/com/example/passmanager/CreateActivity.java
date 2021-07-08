package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);
        firstPassField = findViewById(R.id.firstPassEditText);
        secondPassField = findViewById(R.id.secondPassEditText);
        notMatchingTextView = findViewById(R.id.notMatchingTextView);
        entryVm = new ViewModelProvider(this).get(EntryViewModel.class);
    }

    public void goToCreate(View view) {
        // TODO: Make sure these passwords aren't empty.
        char[] pass1 = firstPassField.getText().toString().toCharArray();
        char[] pass2 = secondPassField.getText().toString().toCharArray();

        if (!Arrays.equals(pass1, pass2)) {
            notMatchingTextView.setText(R.string.passwords_not_matching);
        } else {
            entryVm.create(getApplication(), pass1);
            entryVm.close();
            finish();
        }
    }
}