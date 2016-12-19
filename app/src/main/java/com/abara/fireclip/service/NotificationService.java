package com.abara.fireclip.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;

import com.abara.fireclip.R;
import com.abara.fireclip.SplashActivity;
import com.abara.fireclip.receiver.AcceptClipActionReceiver;
import com.abara.fireclip.receiver.AcceptFileActionReceiver;
import com.abara.fireclip.util.AndroidUtils;
import com.abara.fireclip.util.FireClipUtils;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Created by abara on 10/12/16.
 */

public class NotificationService extends FirebaseMessagingService {

    public static final int NOTIF_CLIP_ID = 9976;
    public static final int NOTIF_FILE_ID = 3314;

    private SharedPreferences preferences;
    private String deviceName;

    private NotificationManagerCompat manager;
    private NotificationCompat.Builder builder;

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = AndroidUtils.getPreference(this);
        manager = NotificationManagerCompat.from(this);

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Map<String, String> data = remoteMessage.getData();

        String isFile = data.get(FireClipUtils.PUSH_FILE_KEY);
        String from = data.get(FireClipUtils.PUSH_DEVICE_KEY);

        // Get the device name everytime a push notification is received.
        deviceName = AndroidUtils.getDeviceName(this);

        // Do not show notification for same device.
        if (!deviceName.contentEquals(from)) {

            builder = new NotificationCompat.Builder(this);

            boolean doNotDisturb = preferences.getBoolean(AndroidUtils.PREF_SILENT_NOTIF, false);
            if (!doNotDisturb) {
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
            }
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            builder.setSmallIcon(R.drawable.ic_stat_notification);
            builder.setColor(ContextCompat.getColor(this, R.color.colorPrimary));

            Intent appIntent = getAppLaunchIntent();
            builder.setContentIntent(PendingIntent.getActivity(this, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT));

            // Check if the notification is for FILE or CLIP.
            if (isFile.contentEquals(FireClipUtils.TYPE_FILE)) {

                String downloadURL = data.get(FireClipUtils.PUSH_DOWNLOAD_URL_KEY);
                String fileName = data.get(FireClipUtils.PUSH_FILE_NAME_KEY);

                builder.setContentTitle(getResources().getString(R.string.notif_file_title, from));
                builder.setContentText(fileName);
                builder.setTicker(getResources().getString(R.string.notif_file_ticker, from));

                boolean autoAcceptFile = preferences.getBoolean(AndroidUtils.PREF_AUTO_ACCEPT_FILE, false);

                Intent acceptReceiver = AcceptFileActionReceiver.getStarterIntent(this, downloadURL, fileName);

                if (!autoAcceptFile) {
                    PendingIntent acceptPI = PendingIntent.getBroadcast(this, 0, acceptReceiver, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.addAction(R.drawable.ic_action_done, getResources().getString(R.string.action_accept), acceptPI);
                } else {
                    sendBroadcast(acceptReceiver);
                }

                AndroidUtils.addFeedbackAction(this, builder);
                manager.notify(NOTIF_FILE_ID, builder.build());

            } else {

                String content = data.get(FireClipUtils.PUSH_CONTENT_KEY);
                String timestamp = data.get(FireClipUtils.PUSH_TIMESTAMP_KEY);

                boolean autoAccept = preferences.getBoolean(AndroidUtils.PREF_AUTO_ACCEPT, false);

                builder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().bigText(content));

                Intent acceptReceiver = AcceptClipActionReceiver.getStarterIntent(this, content, from, timestamp);

                if (!autoAccept) {
                    builder.setContentTitle(getResources().getString(R.string.notif_text_title, from));
                    builder.setContentText(getResources().getString(R.string.notif_text_content));
                    acceptReceiver.putExtra(AcceptClipActionReceiver.EXTRA_CLEAR_NOTIFICATION, true);
                    PendingIntent acceptPI = PendingIntent.getBroadcast(this, 0, acceptReceiver, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.addAction(R.drawable.ic_action_done, getResources().getString(R.string.action_accept), acceptPI);
                } else {
                    builder.setContentTitle(getResources().getString(R.string.notif_text_auto_accept, from));
                    builder.setContentText(content);
                    sendBroadcast(acceptReceiver);
                }

                AndroidUtils.addFeedbackAction(this, builder);
                manager.notify(NOTIF_CLIP_ID, builder.build());

            }
        }

    }

    /**
     * Method to get the app's launch intent.
     */
    private Intent getAppLaunchIntent() {
        Intent appIntent = new Intent(this, SplashActivity.class);
        appIntent.setAction(Intent.ACTION_MAIN);
        appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return appIntent;
    }


}
