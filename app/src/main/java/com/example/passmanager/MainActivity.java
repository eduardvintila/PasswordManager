package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private TextView validationMsg;
    private EditText masterPassField;
    private EntryViewModel entryVm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        validationMsg = findViewById(R.id.validationTextView);
        masterPassField = findViewById(R.id.editTextPassword);

        entryVm = new ViewModelProvider(this).get(EntryViewModel.class);
    }

    public void auth(View view) {
        char[] pass = masterPassField.getText().toString().toCharArray();
        entryVm.open(getApplication(), pass);

        if (!entryVm.isValidMasterPass()) {
            validationMsg.setText(R.string.invalidPassMsg);
        } else {
            validationMsg.setText(R.string.validPassMsg);
        }
    }
}