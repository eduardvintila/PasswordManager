package com.example.passmanager.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class DriveHelper {

    private final GoogleSignInOptions gso;
    private Drive googleDriveService;
    private final ExecutorService fileOpsExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService signInExecutor = Executors.newSingleThreadExecutor();
    private final CountDownLatch latch = new CountDownLatch(2);



    public static final String DEFAULT_MIME_TYPE = "application/vnd.sqlite3";

    private static DriveHelper INSTANCE;

    private DriveHelper() {
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

    public void signIn(ActivityResultLauncher<Intent> launcher, Context context) {
        GoogleSignInClient client = GoogleSignIn.getClient(context, gso);
        launcher.launch(client.getSignInIntent());
    }

    public void onSignInResult(Intent result, Context context) {
        signInExecutor.execute(() -> {
            Log.d("DriveHelper", "enter sign in");
            GoogleSignIn.getSignedInAccountFromIntent(result)
                    .addOnSuccessListener(signInExecutor, googleAccount-> {
                        GoogleAccountCredential credential =
                                GoogleAccountCredential.usingOAuth2(
                                        context, Collections.singleton(DriveScopes.DRIVE_APPDATA));
                        credential.setSelectedAccount(googleAccount.getAccount());
                        googleDriveService = new Drive.Builder(
                                AndroidHttp.newCompatibleTransport(),
                                new GsonFactory(),
                                credential)
                                .setApplicationName("Password Manager")
                                .build();
                        Log.d("DriveHelper", "Autentificare cu succes Google");
                        latch.countDown();
                    })
                    .addOnFailureListener(signInExecutor, fail -> Log.d("DriveHelper",
                            "Autentificare esuata Google", fail));
        });
        latch.countDown();
    }

    public void saveFile(java.io.File filePath) {
        try {
            latch.await();
            fileOpsExecutor.execute(() -> {
                if (filePath.exists()) {
                    File fileMetadata = new File();
                    fileMetadata.setName(filePath.getName());
                    fileMetadata.setParents(Collections.singletonList("appDataFolder"));
                    FileContent mediaContent = new FileContent(DEFAULT_MIME_TYPE, filePath);
                    try {
                        boolean fileUpdated = false;
                        String nextPageToken = null;

                        do {
                            if (googleDriveService == null) {
                                Log.d("DriveHelper", "saveFile: gds e null");
                            } else if (googleDriveService.files() == null){
                                Log.d("DriveHelper", "saveFile: files e null");
                            }
                            FileList files = googleDriveService.files().list()
                                    .setSpaces("appDataFolder")
                                    .setQ(String.format("name='%s'", filePath.getName()))
                                    .setFields("nextPageToken, files(id, name)")
                                    .setPageToken(nextPageToken)
                                    .setPageSize(10)
                                    .execute();

                            for (File foundFile : files.getFiles()) {
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

                            nextPageToken = files.getNextPageToken();
                        } while (nextPageToken != null);

                        if (!fileUpdated) {
                            File driveFile = googleDriveService.files().create(fileMetadata, mediaContent)
                                    .setFields("id")
                                    .execute();
                            Log.d("DriveHelper", "Drive file id: " +  driveFile.getId());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void getFile(java.io.File file) {
        try {
            latch.await();
            fileOpsExecutor.execute(() -> {
                try {
                    List<File> files = googleDriveService.files().list()
                            .setSpaces("appDataFolder")
                            .setQ(String.format("name='%s'", file.getName()))
                            .setFields("nextPageToken, files(id, name)")
                            .setPageSize(1)
                            .execute()
                            .getFiles();

                    if (files.size() == 1) {
                        File foundFile = files.get(0);
                        Log.d("AuthActivity", "found database: " + foundFile.getId());
                        OutputStream outputStream = new FileOutputStream(file);
                        googleDriveService.files().get(foundFile.getId())
                                .executeMediaAndDownloadTo(outputStream);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
