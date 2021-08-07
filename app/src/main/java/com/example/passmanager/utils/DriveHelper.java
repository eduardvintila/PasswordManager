package com.example.passmanager.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.DialogFragment;

import com.example.passmanager.R;
import com.example.passmanager.dialogs.LoadingDialogFragment;
import com.example.passmanager.model.ApplicationDatabase;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
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
    private final ListeningExecutorService listeningFileOpsExecutor =
            MoreExecutors.listeningDecorator(fileOpsExecutor);

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
        GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(context);
        if (!GoogleSignIn.hasPermissions(acc, gso.getScopeArray()) || googleDriveService == null) {
            // TODO: Should reset latch?
            GoogleSignInClient client = GoogleSignIn.getClient(context, gso);
            signInLauncher.launch(client.getSignInIntent());
        }
    }

    /**
     * Handle the Google sign in result. If authentication was successful, setup a handle for
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
                                new NetHttpTransport(),
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
     * Find a file in Google Drive.
     *
     * @param name The name of the file.
     * @return A {@link ListenableFuture} which will contain a {@link Pair} of the file's ID in the
     * Drive and it's last modification time; or a {@code null} {@link Pair} if the file could
     * not be found.
     */
    public ListenableFuture<Pair<String, DateTime>> findFile(String name) {
        return listeningFileOpsExecutor.submit(() -> {
            try {
                // Wait for sign in completion.
                signInLatch.await();

                if (googleDriveService != null) {
                    // Get the list of all the files in the Drive with the desired name.
                    List<File> files = googleDriveService.files().list()
                            .setSpaces("appDataFolder")
                            .setQ(String.format("name='%s'", name))
                            .setOrderBy("modifiedTime desc")
                            .setFields("nextPageToken, files(id, name, modifiedTime)")
                            .setPageSize(1)
                            .execute()
                            .getFiles();

                    if (files.size() > 0) {
                        // Get the most recent file.
                        File foundFile = files.get(0);
                        return Pair.create(foundFile.getId(), foundFile.getModifiedTime());
                    } else {
                        Log.d("DriveHelper", "File not found!");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Download a file from Google Drive.
     *
     * @param fileId The file's ID in the Drive.
     * @param downloadPath Filesystem path to the download location (should include the file name).
     * @return A {@link ListenableFuture} which will indicate whether or not the download was
     * successful.
     */
    public ListenableFuture<Boolean> getFile(String fileId, java.io.File downloadPath) {
        return listeningFileOpsExecutor.submit(() -> {
            try {
                // Wait for sign in completion.
                signInLatch.await();

                OutputStream outputStream = new FileOutputStream(downloadPath);
                googleDriveService.files().get(fileId)
                        .executeMediaAndDownloadTo(outputStream);

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Save a local file to Google Drive.
     *
     * @param filePath The filesystem path to the local file.
     * @return A {@link ListenableFuture} which will indicate whether or not the upload was
     * successful.
     */
    public ListenableFuture<Boolean> saveFile(java.io.File filePath) {
        return listeningFileOpsExecutor.submit(() -> {
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
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public interface FinishDownloadCallback {
        void onFinishDownload(boolean success);
    }

    public interface FinishUploadCallback {
        void onFinishUpload(boolean success);
    }


    /**
     * Load a confirmation dialog box for uploading a local database to Google Drive.
     *
     * @param context Current application context.
     * @param localDbPath Filesystem path to the local database.
     * @param uploadCallback Callback for a finished upload.
     * @param mainExecutor Main thread executor for executing the callback.
     */
    private void showDriveDbUploadConfirmationDialog(Context context,
                                                     java.io.File localDbPath,
                                                     FinishUploadCallback uploadCallback,
                                                     Executor mainExecutor) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.confirmation_upload_db_title)
                .setMessage(R.string.confirmation_upload_db)
                .setPositiveButton(R.string.upload, (dialog, which) -> {
                    // Upload the local database to the Drive.
                    ListenableFuture<Boolean> uploadFuture =
                            saveFile(localDbPath);

                    DialogFragment uploadDbLoadingDialog = displayLoadingDialog(context);
                    if (uploadCallback != null) {
                        uploadFuture.addListener(() -> {
                            // Upload finished.
                            if (uploadDbLoadingDialog != null) {
                                // Dismiss the loading dialog.
                                uploadDbLoadingDialog.dismiss();
                            }
                            // Check if the upload was successful.
                            boolean success;
                            try {
                                success = uploadFuture.get();
                            } catch (Exception e) {
                                e.printStackTrace();
                                success = false;
                            }
                            uploadCallback.onFinishUpload(success);
                        }, mainExecutor);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * Load a confirmation dialog box for downloading a remote database from Google Drive.
     *
     * @param context Current application context.
     * @param driveDbId The remote database's ID in the Drive.
     * @param downloadPath Filesystem path to the download location.
     * @param downloadCallback Callback for a finished download.
     * @param mainExecutor Main thread executor for executing the callback.
     */
    private void showDriveDbDownloadConfirmationDialog(Context context,
                                                       String driveDbId, java.io.File downloadPath,
                                                       FinishDownloadCallback downloadCallback,
                                                       Executor mainExecutor) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.confirmation_download_db_title)
                .setMessage(R.string.confirmation_download_db)
                .setPositiveButton(R.string.download, (dialog, which) -> {
                    // Download the remote database from Google Drive.
                    ListenableFuture<Boolean> downloadFuture =
                            getFile(driveDbId, downloadPath);

                    DialogFragment downloadDbLoadingDialog = displayLoadingDialog(context);
                    if (downloadCallback != null) {
                        downloadFuture.addListener(() -> {
                            // Download finished.
                            if (downloadDbLoadingDialog != null) {
                                // Dismiss the loading dialog.
                                downloadDbLoadingDialog.dismiss();
                            }
                            // Check if the download was successful.
                            boolean success;
                            try {
                                success = downloadFuture.get();
                            } catch (Exception e) {
                                e.printStackTrace();
                                success = false;
                            }
                            downloadCallback.onFinishDownload(success);
                        }, mainExecutor);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();

    }

    /**
     * Load a dialog box with options for synchronizing the database with Google Drive.
     *
     * @param context Current application context.
     * @param uploadCallback Callback for a finished upload.
     * @param downloadCallback Callback for a finished download.
     */
    public void showDriveDbSyncDialog(Context context,
                                      FinishUploadCallback uploadCallback,
                                      FinishDownloadCallback downloadCallback) {
        try {
            // Executor for the main (UI) thread.
            Executor mainThreadExecutor = ContextCompat.getMainExecutor(context);

            // Local database information.
            java.io.File localDbPath = context.getDatabasePath(ApplicationDatabase.DB_NAME);
            String localDbName = localDbPath.getName();

            // Find an existing database in the Drive.
            ListenableFuture<Pair<String, DateTime>> futureSearch = findFile(localDbName);

            DialogFragment findDbLoadingDialog = displayLoadingDialog(context);
            futureSearch.addListener(() -> {
                // Search complete.
                if (findDbLoadingDialog != null) { findDbLoadingDialog.dismiss(); }
                try {
                    String dialogTitle;
                    String dialogMessage;
                    boolean foundDbOnDrive;
                    boolean foundDbLocally;
                    String driveDbId = null;

                    // Get the search result, which consists in a pair of the db file's ID in the
                    // Drive and the time of last modification.
                    // TODO: Get the file size, too.
                    Pair<String, DateTime> result = futureSearch.get();

                    if (result != null && (driveDbId = result.first) != null
                            && result.second != null) {
                        foundDbOnDrive = true;

                        // Convert the time to the local timezone.
                        long millis = result.second.getValue();
                        Instant instant = Instant.ofEpochMilli(millis);
                        LocalDateTime driveDbLastModified = LocalDateTime.ofInstant(instant,
                                ZoneId.systemDefault());

                        dialogTitle = context.getString(R.string.db_drive_found);
                        dialogMessage =
                                String.format(context.getString(R.string.db_drive_found_format),
                                driveDbLastModified.toString());


                    } else {
                        foundDbOnDrive = false;
                        dialogTitle = context.getString(R.string.db_drive_not_found);
                        dialogMessage = context.getString(R.string.db_not_found_drive_long);
                    }

                    if (localDbPath.exists()) {
                        foundDbLocally = true;

                        // Get the time of last modification of the local db in the local timezone.
                        long millis = localDbPath.lastModified();
                        Instant instant = Instant.ofEpochMilli(millis);
                        LocalDateTime localDbLastModified = LocalDateTime.ofInstant(instant,
                                ZoneId.systemDefault());

                        dialogMessage +=
                                String.format(context.getString(R.string.db_found_locally_format),
                                localDbLastModified.toString());
                    } else {
                        foundDbLocally = false;
                        dialogMessage += context.getString(R.string.db_not_found_locally);
                    }

                    String finalDriveDbId = driveDbId;
                    AlertDialog alertDialog = new MaterialAlertDialogBuilder(context)
                            .setTitle(dialogTitle)
                            .setMessage(dialogMessage)
                            .setPositiveButton(R.string.upload_db_in_cloud, (dialog, which) -> {
                                    showDriveDbUploadConfirmationDialog(context, localDbPath,
                                            uploadCallback, mainThreadExecutor);
                                })
                            .setNegativeButton(R.string.download_db_from_cloud, (dialog, which) -> {
                                    showDriveDbDownloadConfirmationDialog(context, finalDriveDbId,
                                            localDbPath, downloadCallback, mainThreadExecutor);
                                })
                            .setNeutralButton(R.string.close, null)
                            .show();

                    if (!foundDbOnDrive) {
                        // Disable Download button.
                        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                    }

                    if (!foundDbLocally) {
                        // Disable Upload button.
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, mainThreadExecutor);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DialogFragment displayLoadingDialog(Context context) {
        try {
            // Display a loading dialog box.
            AppCompatActivity activity = (AppCompatActivity) context;
            DialogFragment loadingDialog = new LoadingDialogFragment();
            loadingDialog.show(activity.getSupportFragmentManager(), "dialogLoading");
            return loadingDialog;
        } catch (ClassCastException e) {
            // Could not cast context to AppCompatActivity
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Set the text of a Google Sign In button.
     *
     * @param btn The Google Sign In button.
     * @param text The text to be set.
     */
    public static void setGoogleButtonText(SignInButton btn, CharSequence text) {
        for (int i = 0; i < btn.getChildCount(); i++) {
            View view = btn.getChildAt(0);
            if (view instanceof TextView) {
                ((TextView) view).setText(text);
                return;
            }
        }
    }
}
