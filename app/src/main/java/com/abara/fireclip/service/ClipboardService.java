package com.abara.fireclip.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.abara.fireclip.R;
import com.abara.fireclip.SplashActivity;
import com.abara.fireclip.receiver.AcceptActionReceiver;
import com.abara.fireclip.util.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jaredrummler.android.device.DeviceName;

import java.util.Date;
import java.util.Map;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

/**
 * Created by abara on 30/07/16.
 */

/*
* Service to receive clips from other devices.
* */
public class ClipboardService extends Service {

    public static final int NEW_CLIP_NOTIFICATION_ID = 9976;
    private static final String TAG = ClipboardService.class.getSimpleName();
    private ClipboardManager clipboardManager;
    private ClipboardManager.OnPrimaryClipChangedListener clipChangedListener;
    private ClipData.Item clipItem;
    private ClipData clipData;

    private DatabaseReference userClipRef;
    private SharedPreferences preferences;
    private ValueEventListener clipValueListener;

    private String clipboardText = "", deviceName;

    private NotificationManagerCompat manager;
    private NotificationCompat.WearableExtender wearableExtender;

    @Override
    public void onCreate() {
        super.onCreate();

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        manager = NotificationManagerCompat.from(this);

        clipData = clipboardManager.getPrimaryClip();

        // Notification support for wearables
        wearableExtender = new NotificationCompat.WearableExtender()
                .setBackground(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notification_large));

        if (firebaseUser != null) {

            userClipRef = FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid()).child("clip");

            // Listen for local clipboard changes.
            clipChangedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {

                    getDeviceName();

                    if (clipboardManager.hasPrimaryClip()) {

                        // Check if the clipboard has plain text.
                        // It's not possible to have binary items (like images etc) in clipboard on Android.
                        ClipDescription clipDescription = clipboardManager.getPrimaryClipDescription();
                        if (clipDescription.hasMimeType(MIMETYPE_TEXT_PLAIN)) {

                            clipData = clipboardManager.getPrimaryClip();
                            clipItem = clipData.getItemAt(0);
                            CharSequence clipText = clipItem.getText();

                            // Check if there are some text in clipboard.
                            if (clipText != null) {

                                // Update only if the clip received is not in the clipboard already.
                                if (!clipboardText.contentEquals(clipText)) {
                                    Map<String, Object> dataMap = Utils.generateMapClip(clipText.toString(), deviceName);

                                    userClipRef.setValue(dataMap);
                                    clipboardText = clipText.toString();

                                    // Update local history as well.
                                    Utils.updateRealmDB(clipboardText, deviceName, new Date().getTime());

                                    Log.d(TAG, "onPrimaryClipChanged: Saved data is text : " + clipItem.toString());
                                    Log.d(TAG, "onPrimaryClipChanged: Database updated!");
                                } else {
                                    Log.d(TAG, "onPrimaryClipChanged: Text is already in the clipboard!");
                                }

                            } else {
                                Log.d(TAG, "onPrimaryClipChanged: There is no text in clipboard!");
                            }

                        } else {
                            Log.d(TAG, "onPrimaryClipChanged: Content is not a text!");
                        }

                    }

                }
            };

            clipboardManager.addPrimaryClipChangedListener(clipChangedListener);

            /*
            * Listen for new clips from Firebase database.
            * */
            clipValueListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if (dataSnapshot.getValue() != null) {

                        String text = (String) dataSnapshot.child(Utils.DATA_MAP_CONTENT).getValue();
                        String from = (String) dataSnapshot.child(Utils.DATA_MAP_FROM).getValue();
                        Long timestamp = (Long) dataSnapshot.child(Utils.DATA_MAP_TIME).getValue();

                        // Do not show notification if the clip is from same device.
                        if (!clipboardText.contentEquals(text)) {

                            getDeviceName();
                            boolean autoAccept = preferences.getBoolean(Utils.AUTO_ACCEPT_KEY, false);

                            if (!deviceName.contentEquals(from)) {

                                NotificationCompat.Builder builder = new NotificationCompat.Builder(ClipboardService.this);
                                builder.setContentTitle(from);
                                builder.setContentText("You received a new clip.");
                                boolean silentNotif = preferences.getBoolean(Utils.SILENT_NOTIF_KEY,false);
                                if(!silentNotif){
                                    builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                                    builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
                                }
                                builder.setPriority(NotificationCompat.PRIORITY_HIGH);
                                builder.setSmallIcon(R.drawable.ic_stat_notification);
                                builder.setColor(ContextCompat.getColor(ClipboardService.this, R.color.colorPrimary));
                                builder.extend(wearableExtender);

                                builder.setStyle(new android.support.v4.app.NotificationCompat.BigTextStyle().bigText(text));

                                Intent appIntent = new Intent(ClipboardService.this, SplashActivity.class);
                                appIntent.setAction(Intent.ACTION_MAIN);
                                appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                                appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                builder.setContentIntent(PendingIntent.getActivity(ClipboardService.this, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT));

                                Intent acceptReceiver = new Intent(ClipboardService.this, AcceptActionReceiver.class);
                                acceptReceiver.putExtra(AcceptActionReceiver.TEXT_EXTRA, text);
                                acceptReceiver.putExtra(AcceptActionReceiver.FROM_EXTRA, from);
                                acceptReceiver.putExtra(AcceptActionReceiver.TIMESTAMP_EXTRA, timestamp);
                                if (!autoAccept) {
                                    acceptReceiver.putExtra(AcceptActionReceiver.CLEAR_NOTIF_EXTRA, true);
                                    PendingIntent acceptPI = PendingIntent.getBroadcast(ClipboardService.this, 0, acceptReceiver, PendingIntent.FLAG_UPDATE_CURRENT);
                                    builder.addAction(R.drawable.ic_done_white_24dp, "Accept", acceptPI);
                                } else {
                                    sendBroadcast(acceptReceiver);
                                    Log.d(TAG, "Updated CB and DB : " + clipboardText + ", NEW TEXT : " + text);
                                }

                                clipboardText = text;

                                manager.notify(NEW_CLIP_NOTIFICATION_ID, builder.build());
                                Log.d(TAG, "Notification created : " + deviceName + " : " + from);
                            }

                        }

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "onCancelled: " + databaseError.getMessage());
                }
            };

            userClipRef.addValueEventListener(clipValueListener);

        }
    }

    /*
    * get and set the device name to the variable.
    * */
    private void getDeviceName() {
        deviceName = preferences.getString(Utils.DEVICE_NAME_KEY, DeviceName.getDeviceName());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
    * Get the current text from the clipboard and
    * store it onto clipboardText variable.
    * */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (clipboardManager.hasPrimaryClip()) {
            if (clipboardManager.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
                clipData = clipboardManager.getPrimaryClip();
                clipItem = clipData.getItemAt(0);
                clipboardText = clipItem.getText().toString();
            }
        }
        return START_STICKY;
    }

    /*
    * Cleanup when destroy.
    * */
    @Override
    public void onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(clipChangedListener);
        userClipRef.removeEventListener(clipValueListener);
        manager.cancel(NEW_CLIP_NOTIFICATION_ID);
        Log.d(TAG, "onDestroy: Service destroyed!");
        super.onDestroy();
    }
}
