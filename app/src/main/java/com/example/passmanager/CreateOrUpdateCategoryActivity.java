package com.example.passmanager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContentResolverCompat;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.passmanager.model.Category;
import com.example.passmanager.viewmodel.ApplicationViewModel;

import java.util.Collections;
import java.util.List;

public class CreateOrUpdateCategoryActivity extends AppCompatActivity {

    private EditText categoryNameField;
    private ActivityResultLauncher<String[]> activityLauncher;
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
        activityLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                uri -> {
                    iconUri = uri;
                    // Set the URI to be persistable across device reboots.
                    getContentResolver().takePersistableUriPermission(iconUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    // Set the icon URI to the image selected.
                    iconImageView.setImageURI(iconUri);
                });
    }

    public void saveCategory() {
        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);
        viewmodel.insertCategories(new Category(categoryNameField.getText().toString(), iconUri));
        finish();
    }

    void chooseIcon() {
        String[] arr = {"image/*"};
        activityLauncher.launch(arr);
    }
}