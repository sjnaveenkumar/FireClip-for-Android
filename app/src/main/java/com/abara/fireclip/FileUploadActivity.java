package com.abara.fireclip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.abara.fireclip.util.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jaredrummler.android.device.DeviceName;

import java.util.Map;

/**
 * Created by abara on 01/11/16.
 */

/*
* This activity is shown as a dialog for uploading files to FireClip.
* Once uploaded, all devices are notified.
* */
public class FileUploadActivity extends AppCompatActivity implements View.OnClickListener {

    private static final long FILE_SIZE_LIMIT = 5 * 1024 * 1024;

    private AppCompatButton cancelBtn;
    private ProgressBar progressBar;
    private AppCompatTextView statusText;

    private UploadTask uploadTask;
    private FirebaseUser user;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_file_upload);


        statusText = (AppCompatTextView) findViewById(R.id.file_upload_status);

        // To check whether the user is signed in or not.
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {

            // Upload only when device is online.
            if (Utils.isOnline(this)) {

                // Cancel button
                cancelBtn = (AppCompatButton) findViewById(R.id.file_upload_cancel);
                cancelBtn.setOnClickListener(this);

                // Upload status text
                statusText.setText(getResources().getString(R.string.file_upload_checking));

                // Get the intent.
                Intent intent = getIntent();

                // Get the file uri from intent.
                Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

                // Default file name.
                String fileName = "unknown_name";
                long fileSize = 0;

                // Getting file name and from content resolver.
                Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);
                cursor.moveToFirst(); // TODO: Getting some error here on some file explorers (like CM filemanager). Have to fix that!
                if (cursor.getCount() > 0) {
                    fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    fileSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                }

                // Check for file name and size.
                if (fileSize != 0) {

                    // Upload only if file size is within FILE_SIZE_LIMIT
                    if (fileSize <= FILE_SIZE_LIMIT) {

                        statusText.setText(getResources().getString(R.string.file_upload_copying));

                        // Set file name to custom meta data.
                        // So that other devices receive with the same file name.
                        StorageMetadata metaData = new StorageMetadata.Builder()
                                .setContentType(intent.getType())
                                .setCustomMetadata("filename", fileName)
                                .build();

                        // Progressbar
                        progressBar = (ProgressBar) findViewById(R.id.file_upload_progress);

                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference userRef = storage.getReference("users").child(user.getUid()).child("my_file");
                        uploadTask = userRef.putFile(fileUri, metaData);

                        // Firebase Storage upload task
                        final String finalFileName = fileName;
                        uploadTask.addOnFailureListener(this, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Show a toast in case some error occurs!
                                Toast.makeText(FileUploadActivity.this, "Couldn't copy file, try again later!", Toast.LENGTH_SHORT).show();
                                Log.e(this.getClass().getSimpleName(), e.getMessage());
                            }
                        }).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                // Sync to all devices.
                                updateRealTimeDatabase(taskSnapshot, finalFileName);

                            }

                        }).addOnProgressListener(this, new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                // Get the progress in percentage.
                                int progress = (int) ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());

                                // Animate progress bar for versions >= N
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    progressBar.setProgress(progress, true);
                                } else {
                                    progressBar.setProgress(progress);
                                }
                            }
                        });

                    } else {

                        statusText.setText(getResources().getString(R.string.file_upload_large_file_size));

                    }
                } else {

                    statusText.setText(getResources().getString(R.string.file_upload_invalid_file));

                }

            } else {

                // Device is offline.
                statusText.setText(getResources().getString(R.string.file_upload_offline));

            }

        } else {

            // Prompt to sign in.
            Toast.makeText(this, "Sign in to copy files!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, SplashActivity.class));
            finish();

        }
    }

    private void updateRealTimeDatabase(UploadTask.TaskSnapshot taskSnapshot, String fileName) {

        statusText.setText(getResources().getString(R.string.file_upload_syncing));
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String deviceName = preferences.getString(Utils.DEVICE_NAME_KEY, DeviceName.getDeviceName());

        Uri downloadURL = taskSnapshot.getDownloadUrl();
        Map<String, Object> map = Utils.generateMapClip(downloadURL.toString(), deviceName, fileName);

        DatabaseReference fileRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("file");
        fileRef.setValue(map).addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                Toast.makeText(FileUploadActivity.this, "File copied!", Toast.LENGTH_SHORT).show();
                finish();

            }
        });

    }

    @Override
    public void onClick(View view) {

        // Cancel the task if progress is running.
        if (uploadTask != null && uploadTask.isInProgress()) {
            uploadTask.cancel();
            Toast.makeText(this, "Cancelled!", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
