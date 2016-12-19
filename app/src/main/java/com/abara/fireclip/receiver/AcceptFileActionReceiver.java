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

import com.abara.fireclip.R;
import com.abara.fireclip.service.NotificationService;
import com.abara.fireclip.util.AndroidUtils;
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
 * BroadcastReceiver to download the files.
 * <p>
 * Created by abara on 01/11/16.
 */
public class AcceptFileActionReceiver extends BroadcastReceiver {

    private static final String EXTRA_URL = "url_file";
    private static final String EXTRA_FILENAME = "file_name";
    private static final long MIN_UPDATE_INTERVAL = 1200;
    private static final int DOWNLOAD_NOTIF_ID = 4;
    private StorageReference fileRef;

    private String fileName, mimeType;
    private Context context;

    private NotificationCompat.Builder builder;
    private NotificationManagerCompat manager;

    // Variable to control the download notification's progress.
    private long lastUpdateTime = System.currentTimeMillis();

    public static Intent getStarterIntent(Context context, String url, String fileName) {
        Intent acceptIntent = new Intent(context, AcceptFileActionReceiver.class);
        acceptIntent.putExtra(EXTRA_URL, url);
        acceptIntent.putExtra(EXTRA_FILENAME, fileName);
        return acceptIntent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        this.context = context;
        manager = NotificationManagerCompat.from(context);

        String url = intent.getStringExtra(EXTRA_URL);
        fileName = intent.getStringExtra(EXTRA_FILENAME);

        fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(url);

        // Getting meta data of the file.
        fileRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {

                mimeType = storageMetadata.getContentType();

                downloadFile();
                manager.cancel(NotificationService.NOTIF_FILE_ID);

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

        builder = new NotificationCompat.Builder(context);

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
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(theFile)));

                // Update notification.
                updateNotification("File saved successfully!");
                builder.setAutoCancel(true);

                // View the file after tapping on the notification.
                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                openIntent.setDataAndType(fileUri, mimeType);
                openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                AndroidUtils.addFeedbackAction(context, builder);

                PendingIntent pi = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                builder.setContentIntent(pi);

                manager.notify(DOWNLOAD_NOTIF_ID, builder.build());

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                // Update notification.
                updateNotification("Failed to get file!");

                AndroidUtils.addFeedbackAction(context, builder);

                manager.notify(DOWNLOAD_NOTIF_ID, builder.build());

                e.printStackTrace();

            }
        });

    }

    /*
    * Method to update notification.
    * */
    private void updateNotification(String content) {
        builder.setContentText(content);
        builder.setProgress(0, 0, false);
        builder.setOngoing(false);
        builder.setTicker(content);
    }

}
