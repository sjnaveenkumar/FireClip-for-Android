package com.abara.fireclip;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import com.abara.fireclip.util.AndroidUtils;
import com.abara.fireclip.util.FireClipUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;

/**
 * <p>Activity shown as dialog for uploading files.
 * Validate the file and upload it.
 * If upload is success, notify all the devices.</p>
 * <p>
 * Created by abara on 01/11/16.
 */
public class FileUploadActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * File size limit is 5MB.
     */
    private static final long FILE_SIZE_LIMIT = 5 * 1024 * 1024;

    private AppCompatButton cancelBtn;
    private ProgressBar progressBar;
    private AppCompatTextView statusText;

    private UploadTask uploadTask;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_file_upload);

        statusText = (AppCompatTextView) findViewById(R.id.file_upload_status);
        cancelBtn = (AppCompatButton) findViewById(R.id.file_upload_cancel);
        cancelBtn.setOnClickListener(this);

        // To check whether the user is signed in or not.
        if (FireClipUtils.isUserSignedIn()) {

            // Upload only when device is online.
            if (AndroidUtils.isOnline(this)) {

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
                try {
                    cursor.moveToFirst(); // TODO: Getting some error here on some apps (like CM filemanager and whatsapp). Have to fix that!
                    if (cursor.getCount() > 0) {
                        fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        fileSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                    }
                    cursor.close();
                } catch (Exception e) {
                    Toast.makeText(this, "File copy failed!", Toast.LENGTH_SHORT).show();
                    return;
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

                        StorageReference fileRef = FireClipUtils.getFileStorageReference(fileName);
                        uploadTask = fileRef.putFile(fileUri, metaData);

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
                            public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {

                                // Sync to all devices.
                                statusText.setText(getResources().getString(R.string.file_upload_syncing));
                                FireClipUtils.syncFile(FileUploadActivity.this, taskSnapshot.getDownloadUrl(), finalFileName)
                                        .addOnCompleteListener(FileUploadActivity.this, new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                                Toast.makeText(FileUploadActivity.this, "File copied!", Toast.LENGTH_SHORT).show();
                                                try {
                                                    FireClipUtils.sendFileNotification(FileUploadActivity.this, taskSnapshot.getDownloadUrl(), finalFileName);
                                                } catch (JSONException e) {
                                                    Toast.makeText(FileUploadActivity.this, "Copy failed!", Toast.LENGTH_SHORT).show();
                                                }
                                                finish();

                                            }
                                        });

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

    /**
     * Cancel the upload, if running and exit.
     */
    @Override
    public void onClick(View view) {
        if (uploadTask != null && uploadTask.isInProgress()) {
            uploadTask.cancel();
            Toast.makeText(this, "Cancelled!", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
