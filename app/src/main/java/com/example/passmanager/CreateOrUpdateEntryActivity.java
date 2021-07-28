package com.example.passmanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.passmanager.model.Category;
import com.example.passmanager.model.Entry;
import com.example.passmanager.utils.CryptoHelper;
import com.example.passmanager.utils.NetworkHelper;
import com.example.passmanager.viewmodel.ApplicationViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

/**
 * Activity for creating a new entry or modifying an existing one.
 */
public class CreateOrUpdateEntryActivity extends AppCompatActivity {

    private EditText nameField;
    private EditText usernameField;
    private EditText passwordField;
    private EditText descriptionField;
    private EditText linkField;
    private TextInputLayout passwordLayout;

    // Chips for selecting character sets used in password generation.
    private ChipGroup chipGroup;
    private Chip upperAlphaChip;
    private Chip lowerAlphaChip;
    private Chip numericChip;
    private Chip specialChip;

    // Slider for choosing the length of the generated password.
    private Slider passLengthSlider;

    private ApplicationViewModel viewmodel;

    private final int passTextType =
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    private final int passPinType =
            InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD;

    // The encrypted master password used in encrypting the password in the entry.
    private String encryptedMaster;

    // Only used if updating an existing entry.
    private Entry oldEntry;
    private int oldEntryCategoryId = -1;

    // Spinner for choosing a category.
    private Spinner categoriesSpinner;

    // List of all categories for populating the spinner.
    private List<Category> categories;

    // Array adapter for the spinner with the categories' names.
    private ArrayAdapter<CharSequence> arraySpinnerAdapter;

    // Same size as the array adapter, used for storing each category's id.
    private List<Integer> adapterCategoryIds;

    // Id of the chosen category in the spinner.
    private int categoryId = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_or_update_entry);

        nameField = findViewById(R.id.entryNameEditText);
        usernameField = findViewById(R.id.usernameEditText);
        passwordField = findViewById(R.id.entryPassEditText);
        passwordLayout = findViewById(R.id.entryPassTextLayout);
        descriptionField = findViewById(R.id.entryDescriptionEditText);
        linkField = findViewById(R.id.linkEditText);
        chipGroup = findViewById(R.id.chipGroup);
        upperAlphaChip = findViewById(R.id.upperAlphaChip);
        lowerAlphaChip = findViewById(R.id.lowerAlphaChip);
        numericChip = findViewById(R.id.numericChip);
        specialChip = findViewById(R.id.specialChip);
        passLengthSlider = findViewById(R.id.passLengthSlider);


        findViewById(R.id.saveEntryBtn).setOnClickListener(view -> saveEntry());
        findViewById(R.id.generatePassBtn).setOnClickListener(view -> generatePassword());
        findViewById(R.id.checkBreachBtn).setOnClickListener(view -> checkPassword());

        // When the character set or password length has changed, generate a new password.
        upperAlphaChip.setOnCheckedChangeListener((buttonView, isChecked) -> generatePassword());
        lowerAlphaChip.setOnCheckedChangeListener((buttonView, isChecked) -> generatePassword());
        numericChip.setOnCheckedChangeListener((buttonView, isChecked) -> generatePassword());
        specialChip.setOnCheckedChangeListener((buttonView, isChecked) -> generatePassword());
        passLengthSlider.addOnChangeListener((slider, value, fromUser) -> generatePassword());

        findViewById(R.id.textRadioButton).setOnClickListener(view -> {
            onRadioButtonClicked(view);
            passLengthSlider.setValue(R.integer.text_password_default_length);
        });
        findViewById(R.id.pinRadioButton).setOnClickListener(view -> {
            onRadioButtonClicked(view);
            passLengthSlider.setValue(R.integer.pin_password_default_length);
        });

        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file),
                                                            Context.MODE_PRIVATE);
        encryptedMaster = sharedPref.getString(getString(R.string.encrypted_master_key), null);

        Intent prevIntent = getIntent();
        // Check whether we are updating an existing entry or creating a new one
        if (prevIntent.hasExtra(EntriesMenuActivity.EXTRA_ENTRY_ID)) {
            int oldEntryId = prevIntent.getIntExtra(EntriesMenuActivity.EXTRA_ENTRY_ID, 1);
            oldEntryCategoryId = prevIntent.getIntExtra(EntriesMenuActivity.EXTRA_CATEGORY_ID, -1);
            viewmodel.getEntry(oldEntryId).observe(this, entry -> {
                if (oldEntry == null && entry != null) {
                    oldEntry = entry;
                    // When updating an existing entry, load all text fields with the old
                    // information.
                    nameField.setText(entry.name);
                    usernameField.setText(entry.username);
                    descriptionField.setText(entry.description);
                    linkField.setText(entry.link);

                    // Also populate the password field with the plaintext password decrypted
                    // previously.
                    String pass = prevIntent.getStringExtra(EntryActivity.EXTRA_ENTRY_PASSWORD);
                    passwordField.setText(pass);
                }
            });
        }

        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                char[] pass = s.toString().toCharArray();
                boolean isTextPassword = passwordField.getInputType() == passTextType;
                if (isTextPassword && pass.length > 0 &&
                        CryptoHelper.passwordStrongness(pass) < CryptoHelper.PASS_MAX_STRONGNESS) {
                   passwordLayout.setHelperTextEnabled(true);
                   passwordLayout.setHelperText(getString(R.string.password_not_strong));
                } else {
                    passwordLayout.setHelperTextEnabled(false);
                }
            }
        });



        // Create the Spinner with categories.
        arraySpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        arraySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoriesSpinner = findViewById(R.id.categoriesSpinner);
        categoriesSpinner.setAdapter(arraySpinnerAdapter);
        categoriesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                categoryId = adapterCategoryIds.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        viewmodel.getAllCategories().observe(this, categories -> {
            if (categories != null) {
                // Only used when updating an existing entry and for selecting its category in
                // the spinner.
                int posOldEntryCategory = -1;

                this.categories = categories;
                adapterCategoryIds = new ArrayList<>(categories.size());
                arraySpinnerAdapter.clear();
                for (int pos = 0; pos < categories.size(); pos++) {
                    Category c = categories.get(pos);
                    arraySpinnerAdapter.add(c.name);
                    adapterCategoryIds.add(c.categoryId);
                    if (oldEntryCategoryId != -1 && c.categoryId == oldEntryCategoryId) {
                        posOldEntryCategory = pos;
                    }
                }
                if (posOldEntryCategory != -1) {
                    // Set the entry's category in the spinner.
                    categoriesSpinner.setSelection(posOldEntryCategory);
                }
            }
        });
    }

    /**
     * Check if the password has been compromised in a data breach.
     */
    public void checkPassword() {
        // Internet connection required.
        if (NetworkHelper.isInternetConnectionAvailable(this)) {
            Executor executor = Executors.newSingleThreadExecutor();

            // For announcing the UI thread when the network request has finished.
            Handler handler = new Handler(Looper.getMainLooper());

            // Launch the network request on a new thread.
            executor.execute(() -> {
                // Get the password from the field
                Editable editable = passwordField.getText();
                char[] entryPassword = new char[editable.length()];
                editable.getChars(0, editable.length(), entryPassword, 0);

                boolean breached = NetworkHelper.isPassPwned(entryPassword, true);

                // Announce the UI thread that the network operation has finished.
                handler.post(() -> {
                    // Running in the UI Thread.
                    if (breached) {
                        Toast.makeText(CreateOrUpdateEntryActivity.this,
                                R.string.pass_found_in_data_breach, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CreateOrUpdateEntryActivity.this,
                                R.string.pass_not_found_in_data_breach, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    /**
     * Save an entry. Either modify an existing one or create a new one.
     */
    public void saveEntry() {
        char[] plainTextMaster = CryptoHelper.decryptMasterPassword(encryptedMaster);

        // Use the master password and a random salt to encrypt the password for the entry.
        Editable editable = passwordField.getText();
        char[] entryPassword = new char[editable.length()];
        editable.getChars(0, editable.length(), entryPassword, 0);

        byte[] saltBytes = CryptoHelper.generateSalt();
        SecretKey key = CryptoHelper.createPbeKey(plainTextMaster, saltBytes, true);
        String encryptedEntryPassword = CryptoHelper.encrypt(key, entryPassword, true);

        if (oldEntry != null) {
            // Update an existing entry.
            Entry newEntry = oldEntry;
            newEntry.name = nameField.getText().toString();
            newEntry.description = descriptionField.getText().toString();
            newEntry.link = linkField.getText().toString();
            newEntry.username = usernameField.getText().toString();
            newEntry.password = encryptedEntryPassword;
            newEntry.passwordSalt = CryptoHelper.encode(saltBytes);
            newEntry.lastModified = new Date(Calendar.getInstance().getTimeInMillis());
            newEntry.categoryId = categoryId;

            viewmodel.updateEntry(newEntry);
        } else {
            // Create a new entry.
            Entry entry = new Entry(nameField.getText().toString(),
                    descriptionField.getText().toString(),
                    linkField.getText().toString(), usernameField.getText().toString(),
                    encryptedEntryPassword, CryptoHelper.encode(saltBytes),
                    new Date(Calendar.getInstance().getTimeInMillis()), categoryId);

            viewmodel.insertEntry(entry);
        }

        finish();
    }


    /**
     * Generate a random password for the entry.
     */
    public void generatePassword() {
        String generatedPass;
        int passLen = (int) passLengthSlider.getValue();

        if (passwordField.getInputType() == passTextType) {
            // Generate a text password.
            int flags = 0;
            if (lowerAlphaChip.isChecked()) { flags |= CryptoHelper.ALPHA_LOWER_SET; }
            if (upperAlphaChip.isChecked()) { flags |= CryptoHelper.ALPHA_UPPER_SET; }
            if (numericChip.isChecked()) { flags |= CryptoHelper.NUMERIC_SET; }
            if (specialChip.isChecked()) { flags |= CryptoHelper.SPECIAL_SET; }

            if (flags == 0) {
                // No character set selected, show a warning message and don't generate a password.
                Toast.makeText(this, R.string.choose_character_set,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            generatedPass = CryptoHelper.generatePassword(passLen, flags);
        } else {
            // Generate a PIN password.
            generatedPass = CryptoHelper.generatePassword(passLen, CryptoHelper.NUMERIC_SET);
        }

        passwordField.setText(generatedPass);
    }

    /**
     * Called when one of the password type buttons has been pressed.
     *
     * @param view The corresponding radio button.
     */
    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.textRadioButton:
                if (checked) {
                    passwordField.setInputType(passTextType);
                    chipGroup.setVisibility(View.VISIBLE);
                }
                break;

            case R.id.pinRadioButton:
                if (checked) {
                    passwordField.setText("");
                    passwordField.setInputType(passPinType);
                    chipGroup.setVisibility(View.GONE);
                }
                break;
        }
    }
}