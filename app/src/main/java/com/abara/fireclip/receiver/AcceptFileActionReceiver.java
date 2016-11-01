package com.abara.fireclip.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.abara.fireclip.R;
import com.abara.fireclip.service.ClipboardService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;

/**
 * Created by abara on 01/11/16.
 */

public class AcceptFileActionReceiver extends BroadcastReceiver {

    public static final String URL_EXTRA = "url_file";
    public static final String CLEAR_NOTIF_EXTRA = "clear_notif";
    private static final long MIN_UPDATE_INTERVAL = 2000;
    private static final int DOWNLOAD_NOTIF_ID = 4;

    private StorageReference fileRef;
    private String fileName;
    private Context context;

    private NotificationManagerCompat manager;

    private long lastUpdateTime = System.currentTimeMillis();

    @Override
    public void onReceive(Context context, Intent intent) {

        manager = NotificationManagerCompat.from(context);
        this.context = context;

        String url = intent.getStringExtra(URL_EXTRA);
        //String from = intent.getStringExtra(FROM_EXTRA);
        //Long timestamp = intent.getLongExtra(TIMESTAMP_EXTRA, new Date().getTime());

        fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(url);

        fileName = "unknown_file";
        fileRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {

                fileName = storageMetadata.getCustomMetadata("filename");
                Log.d("ABARA", "onSuccess: File name is " + fileName);

                downloadFile();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });

        if (intent.getBooleanExtra(CLEAR_NOTIF_EXTRA, false)) {
            manager.cancel(ClipboardService.NEW_FILE_NOTIFICATION_ID);
        }

    }

    private void downloadFile() {

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentTitle(fileName)
                .setContentText("Getting file...")
                .setSmallIcon(R.drawable.ic_stat_notification);

        File fireclipDir = new File(Environment.getExternalStorageDirectory(), "FireClip files");
        if (!fireclipDir.exists())
            fireclipDir.mkdirs();
        final File theFile = new File(fireclipDir, fileName);
        Log.d("ABARA", "downloadFile: File path " + theFile.getPath());

        fileRef.getFile(theFile).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {

                if ((System.currentTimeMillis() - lastUpdateTime) <= MIN_UPDATE_INTERVAL) return;

                int progress = (int) ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                Log.d("LV", "onProgress: Progress: " + progress);
                builder.setProgress(100, progress, false);

                builder.setOngoing(true);
                manager.notify(DOWNLOAD_NOTIF_ID, builder.build());

                lastUpdateTime = System.currentTimeMillis();
            }
        }).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {

                builder.setContentText("File saved successfully!");
                builder.setProgress(0, 0, false);
                builder.setOngoing(false);
                manager.notify(DOWNLOAD_NOTIF_ID, builder.build());

                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(theFile)));

            }
        }).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {

                builder.setContentText("File saved successfully!");
                builder.setProgress(0, 0, false);
                builder.setOngoing(false);
                manager.notify(DOWNLOAD_NOTIF_ID, builder.build());

                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(theFile)));
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                builder.setContentText("Failed to get file!");
                builder.setProgress(0, 0, false);
                builder.setOngoing(false);
                manager.notify(DOWNLOAD_NOTIF_ID, builder.build());

                e.printStackTrace();

            }
        });

    }

}
