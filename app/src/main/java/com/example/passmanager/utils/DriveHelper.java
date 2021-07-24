package com.example.passmanager.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;

import com.example.passmanager.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Singleton helper class for Google Drive operations.
 */
public class DriveHelper {

    private final GoogleSignInOptions gso;

    // Handle for the Google Drive service.
    private Drive googleDriveService;

    // Used to launch a thread for sign in operations.
    private final ExecutorService signInExecutor = Executors.newSingleThreadExecutor();

    // Used to launch a thread for Google Drive file operations.
    private final ExecutorService fileOpsExecutor = Executors.newSingleThreadExecutor();

    // Latch used in synchronizing the sign in and file ops threads such that no file op is
    // executed until the sign in is complete.
    private final CountDownLatch signInLatch = new CountDownLatch(1);



    public static final String DEFAULT_MIME_TYPE = "application/vnd.sqlite3";

    private static DriveHelper INSTANCE;

    private DriveHelper() {
        // Setup the sign-in options, requesting permissions for viewing the email address and
        // accessing the application data folder.
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.EMAIL))
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA))
                .build();
    }

    public static DriveHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DriveHelper();
        }
        return INSTANCE;
    }

    /**
     * Sign in to Google. Can be called even if the user is already authenticated to Google.
     *
     * @param signInLauncher Used for launching the implicit intent for authentication.
     * @param context Application context.
     */
    public void signIn(ActivityResultLauncher<Intent> signInLauncher, Context context) {
        GoogleSignInClient client = GoogleSignIn.getClient(context, gso);
        signInLauncher.launch(client.getSignInIntent());
    }

    /**
     * Handle the sign in result. If authentication was successful, setup a handle for
     * communicating with the Google Drive service.
     *
     * @param result The sign in result.
     * @param context Application context.
     */
    public void onSignInResult(Intent result, Context context) {
        signInExecutor.execute(() -> {
            GoogleSignIn.getSignedInAccountFromIntent(result)
                    .addOnSuccessListener(signInExecutor, googleAccount-> {
                        GoogleAccountCredential credential =
                                GoogleAccountCredential.usingOAuth2(
                                        context, Collections.singleton(DriveScopes.DRIVE_APPDATA));
                        credential.setSelectedAccount(googleAccount.getAccount());

                        // Connect to Google Drive.
                        googleDriveService = new Drive.Builder(
                                AndroidHttp.newCompatibleTransport(),
                                new GsonFactory(),
                                credential)
                                .setApplicationName("Password Manager")
                                .build();

                        // Mark that the sign in is complete.
                        signInLatch.countDown();
                    })
                    .addOnFailureListener(signInExecutor, fail -> Log.d("DriveHelper",
                            context.getString(R.string.google_authentication_failed), fail));
        });
    }

    /**
     * Save a file to Google Drive.
     *
     * @param filePath The filesystem path of the file.
     */
    public void saveFile(java.io.File filePath) {
        fileOpsExecutor.execute(() -> {
            try {
                // Wait for sign in completion.
                signInLatch.await();

                if (filePath.exists() && googleDriveService != null) {
                    File fileMetadata = new File();
                    fileMetadata.setName(filePath.getName());
                    fileMetadata.setParents(Collections.singletonList("appDataFolder"));
                    FileContent mediaContent = new FileContent(DEFAULT_MIME_TYPE, filePath);
                    boolean fileUpdated = false;
                    String nextPageToken = null;

                    do {
                        // List all the files in the Drive with the same name.
                        FileList files = googleDriveService.files().list()
                                .setSpaces("appDataFolder")
                                .setQ(String.format("name='%s'", filePath.getName()))
                                .setFields("nextPageToken, files(id, name)")
                                .setPageToken(nextPageToken)
                                .setPageSize(10)
                                .execute();

                        for (File foundFile : files.getFiles()) {
                            // If multiple files with the same name are found, update only one
                            // file and delete the rest.
                            if (!fileUpdated) {
                                googleDriveService.files()
                                        .update(foundFile.getId(), null, mediaContent)
                                        .execute();
                                fileUpdated = true;
                            } else {
                                googleDriveService.files()
                                        .delete(foundFile.getId())
                                        .execute();
                            }
                        }

                        // Get the the next page with files.
                        nextPageToken = files.getNextPageToken();
                    } while (nextPageToken != null);

                    if (!fileUpdated) {
                        // If no file with the same name has been found in the Drive, create a
                        // new one.
                        googleDriveService.files().create(fileMetadata, mediaContent)
                                .setFields("id")
                                .execute();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Download a file from Google Drive.
     *
     * @param downloadPath Filesystem path to the download location (should include the file name).
     */
    public void getFile(java.io.File downloadPath) {
        fileOpsExecutor.execute(() -> {
            try {
                // Wait for sign in completion.
                signInLatch.await();

                if (googleDriveService != null) {
                    // Get the list of all the files in the Drive with the desired name.
                    List<File> files = googleDriveService.files().list()
                            .setSpaces("appDataFolder")
                            .setQ(String.format("name='%s'", downloadPath.getName()))
                            .setFields("nextPageToken, files(id, name)")
                            .setPageSize(1)
                            .execute()
                            .getFiles();

                    if (files.size() > 0) {
                        // Get the first found file.
                        File foundFile = files.get(0);
                        OutputStream outputStream = new FileOutputStream(downloadPath);
                        googleDriveService.files().get(foundFile.getId())
                                .executeMediaAndDownloadTo(outputStream);
                    } else {
                        Log.d("DriveHelper", "File not found!");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
