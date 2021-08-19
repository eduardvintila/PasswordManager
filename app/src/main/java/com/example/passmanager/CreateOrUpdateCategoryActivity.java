package com.example.passmanager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.passmanager.model.Category;
import com.example.passmanager.viewmodel.ApplicationViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.io.InputStream;

public class CreateOrUpdateCategoryActivity extends AppCompatActivity {

    // For launching an implicit intent for an image chooser.
    private ActivityResultLauncher<String[]> iconChooserLauncher;
    private ActivityResultLauncher<String> readStoragePermissionLauncher;

    private EditText categoryNameField;
    private Uri iconUri;

    // ImageView for displaying the selected icon.
    private ImageView iconImageView;

    private ApplicationViewModel viewmodel;

    // Only used if updating an existing category.
    private Category oldCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_or_update_category);

        categoryNameField = findViewById(R.id.categoryNameEditText);
        iconImageView = findViewById(R.id.iconImageView);
        findViewById(R.id.chooseIconBtn).setOnClickListener(view -> chooseIcon(view));
        findViewById(R.id.saveCategoryBtn).setOnClickListener(view -> saveCategory());

        viewmodel = new ViewModelProvider(this).get(ApplicationViewModel.class);

        Intent prevIntent = getIntent();
        // Check whether we are updating an existing category or creating a new one.
        if (prevIntent.hasExtra(EntriesMenuActivity.EXTRA_CATEGORY_ID)) {
            setTitle(R.string.modify_existing_category);
            int categoryId = prevIntent.getIntExtra(EntriesMenuActivity.EXTRA_CATEGORY_ID, 1);
            viewmodel.getCategory(categoryId).observe(this, category -> {
                if (oldCategory == null && category != null) {
                    oldCategory = category;
                    categoryNameField.setText(category.name);
                    if (isUriResourceAvailable(category.icon, this)) {
                        iconUri = category.icon;
                    } else {
                        category.icon = null;
                    }
                    iconImageView.setImageURI(category.icon);
                }
            });
        } else {
            setTitle(R.string.add_category);
        }

        // Used for launching an implicit intent to choose a picture.
        iconChooserLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
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

        // Used for launching a prompt for granting permission.
        readStoragePermissionLauncher = registerForActivityResult
                (new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission granted. Launch the image chooser.
                String[] arr = {"image/*"};
                iconChooserLauncher.launch(arr);
            } else {
                // Permission not granted. Show a warning message.
                Toast.makeText(this, R.string.permission_not_granted_image, Toast.LENGTH_SHORT)
                        .show();
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

    /**
     * Launch the implicit intent for choosing an image. Request a permission to browse the storage
     * if it hasn't already been given.
     *
     * @param view The clicked button.
     */
    void chooseIcon(View view) {
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            String[] arr = {"image/*"};
            iconChooserLauncher.launch(arr);
        } else if (shouldShowRequestPermissionRationale(permission)) {
            Snackbar.make(view, R.string.read_storage_permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("Ok", v -> readStoragePermissionLauncher.launch(permission))
                    .show();
        } else {
            readStoragePermissionLauncher.launch(permission);
        }
    }

    /**
     * Verify that a resource pointed by a Uri is still available.
     *
     * @param uri Uri of the resource.
     * @param context Application context.
     * @return true if the content is available; false otherwise.
     */
    public static boolean isUriResourceAvailable(Uri uri, Context context) {
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