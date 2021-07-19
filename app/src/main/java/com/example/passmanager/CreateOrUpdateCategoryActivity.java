package com.example.passmanager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;

import com.example.passmanager.model.Category;
import com.example.passmanager.viewmodel.ApplicationViewModel;

import java.io.InputStream;

public class CreateOrUpdateCategoryActivity extends AppCompatActivity {

    private EditText categoryNameField;
    private ActivityResultLauncher<String[]> activityLauncher;
    private Uri iconUri;
    private ImageView iconImageView;
    private ApplicationViewModel viewmodel;

    // Only used if updating an existing category.
    private Category oldCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_or_update_category);

        categoryNameField = findViewById(R.id.categoryNameEditText);
        findViewById(R.id.chooseIconBtn).setOnClickListener(view -> chooseIcon());
        findViewById(R.id.saveCategoryBtn).setOnClickListener(view -> saveCategory());
        iconImageView = findViewById(R.id.iconImageView);

        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);

        Intent prevIntent = getIntent();
        // Check whether we are updating an existing entry or creating a new one
        if (prevIntent.hasExtra(EntriesMenuActivity.EXTRA_CATEGORY_ID)) {
            int categoryId = prevIntent.getIntExtra(EntriesMenuActivity.EXTRA_CATEGORY_ID, 1);
            viewmodel.getCategory(categoryId).observe(this, category -> {
                if (oldCategory == null && category != null) {
                    oldCategory = category;
                    categoryNameField.setText(category.name);
                    if (isUriContentAvailable(category.icon, this)) {
                        iconUri = category.icon;
                    } else {
                        category.icon = null;
                    }
                    iconImageView.setImageURI(category.icon);
                }
            });
        }

        // Used for launching an implicit intent to choose a picture.
        activityLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        iconUri = uri;
                        // Set the URI to be persistable across device reboots.
                        getContentResolver().takePersistableUriPermission(iconUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        // Set the icon URI to the image selected.
                        iconImageView.setImageURI(iconUri);
                    }
                });
    }

    public void saveCategory() {
        if (oldCategory != null) {
            // Update an existing category.
            Category newCategory = oldCategory;
            newCategory.name = categoryNameField.getText().toString();
            newCategory.icon = iconUri;
            viewmodel.updateCategory(newCategory);
        } else {
            // Create a new category.
            viewmodel.insertCategories(new Category(categoryNameField.getText().toString(), iconUri));
        }

        finish();
    }

    void chooseIcon() {
        String[] arr = {"image/*"};
        activityLauncher.launch(arr);
    }

    public static boolean isUriContentAvailable(Uri uri, Context context) {
        try {
            InputStream inputStream =
                            context.getContentResolver().openInputStream(uri);
            inputStream.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}