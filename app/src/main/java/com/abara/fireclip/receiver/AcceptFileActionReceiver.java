package com.abara.fireclip.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
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

/*
* BroadcastReceiver to download the files.
* */
public class AcceptFileActionReceiver extends BroadcastReceiver {

    public static final String URL_EXTRA = "url_file";
    private static final long MIN_UPDATE_INTERVAL = 1200;
    private static final int DOWNLOAD_NOTIF_ID = 4;

    private StorageReference fileRef;

    private String fileName, mimeType;
    private Context context;

    private NotificationManagerCompat manager;

    // Variable to control the download notification's progress.
    private long lastUpdateTime = System.currentTimeMillis();

    @Override
    public void onReceive(Context context, Intent intent) {

        manager = NotificationManagerCompat.from(context);
        this.context = context;

        String url = intent.getStringExtra(URL_EXTRA);

        fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(url);

        fileName = "unknown_file";

        // Getting meta data of the file.
        fileRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {

                fileName = storageMetadata.getCustomMetadata("filename");
                mimeType = storageMetadata.getContentType();
                Log.d("ABB", "onSuccess: File name is " + fileName);
                Log.d("ABB", "onSuccess: File mime type is " + mimeType);

                downloadFile();
                manager.cancel(ClipboardService.NEW_FILE_NOTIFICATION_ID);

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });

    }

    /*
    * Method to download the file from Firebase storage.
    * */
    private void downloadFile() {

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentTitle(fileName)
                .setContentText("Getting file...")
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary));

        // FireClip files directory
        File fireclipDir = new File(Environment.getExternalStorageDirectory(), "FireClip files");
        if (!fireclipDir.exists())
            fireclipDir.mkdirs();

        // The File
        final File theFile = new File(fireclipDir, fileName);

        fileRef.getFile(theFile).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {

                // Do not update progress frequently.
                if ((System.currentTimeMillis() - lastUpdateTime) <= MIN_UPDATE_INTERVAL) return;

                int progress = (int) ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                Log.d("ABB", "onProgress: Progress: " + progress);
                builder.setProgress(100, progress, false);

                builder.setOngoing(true);
                manager.notify(DOWNLOAD_NOTIF_ID, builder.build());

                // Update the lastUpdateTime to latest.
                lastUpdateTime = System.currentTimeMillis();

            }
        }).addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {

                Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", theFile);

                // Launch the media scanner for downloaded file.
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, fileUri));

                // Update notification.
                updateNotification(builder, "File saved successfully!");
                builder.setAutoCancel(true);

                // View the file after tapping on the notification.
                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setDataAndType(fileUri, mimeType);
                openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                PendingIntent pi = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                builder.setContentIntent(pi);

                manager.notify(DOWNLOAD_NOTIF_ID, builder.build());

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                // Update notification.
                updateNotification(builder, "Failed to get file!");

                manager.notify(DOWNLOAD_NOTIF_ID, builder.build());

                e.printStackTrace();

            }
        });

    }

    /*
    * Method to update notification.
    * */
    private void updateNotification(NotificationCompat.Builder builder, String content) {
        builder.setContentText(content);
        builder.setProgress(0, 0, false);
        builder.setOngoing(false);
        builder.setTicker(content);
    }

}
