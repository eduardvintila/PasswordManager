package com.example.passmanager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;

public class CreateOrUpdateCategoryActivity extends AppCompatActivity {

    private EditText categoryNameField;
    private ActivityResultLauncher<String> activityLauncher;
    private Uri iconUri;
    private ImageView iconImageView;
    private ApplicationViewModel viewmodel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_or_update_category);

        categoryNameField = findViewById(R.id.categoryNameEditText);
        findViewById(R.id.chooseIconBtn).setOnClickListener(view -> chooseIcon());
        findViewById(R.id.saveCategoryBtn).setOnClickListener(view -> saveCategory());
        iconImageView = findViewById(R.id.iconImageView);

        // Used for launching an implicit intent to choose a picture.
        activityLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    iconUri = uri;
                    iconImageView.setImageURI(iconUri); // Set the icon URI to the image selected.
                });
    }

    public void saveCategory() {
        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);
        viewmodel.insertCategories(new Category(categoryNameField.getText().toString(), iconUri));
        finish();
    }

    void chooseIcon() {
        activityLauncher.launch("image/*");
    }
}