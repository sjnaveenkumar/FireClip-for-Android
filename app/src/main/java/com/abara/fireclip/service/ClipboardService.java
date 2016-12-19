package com.abara.fireclip.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import com.abara.fireclip.util.AndroidUtils;
import com.abara.fireclip.util.FireClipUtils;

import org.json.JSONException;

import java.util.Date;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

/**
 * Service to listen for changes in clipboard.
 * Update and send a push notification to all of the user's devices.
 * <p>
 * Created by abara on 30/07/16.
 */
public class ClipboardService extends Service {

    private ClipboardManager clipboardManager;
    private ClipboardManager.OnPrimaryClipChangedListener clipChangedListener;
    private ClipData.Item clipItem;

    private String oldClipText = "", deviceName;

    private NotificationManagerCompat notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        notificationManager = NotificationManagerCompat.from(this);

        // Listen for local clipboard changes.
        clipChangedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {

                // Get device name each time from preference, user might have changed it.
                deviceName = AndroidUtils.getDeviceName(ClipboardService.this);

                if (clipboardManager.hasPrimaryClip()) {

                    // Check if the clipboard has plain text.
                    // It's not possible to have binary items (like images etc) in clipboard on Android.
                    ClipDescription clipDescription = clipboardManager.getPrimaryClipDescription();
                    if (clipDescription.hasMimeType(MIMETYPE_TEXT_PLAIN)) {

                        clipItem = clipboardManager.getPrimaryClip().getItemAt(0);
                        CharSequence clipText = clipItem.getText();

                        // Check if there are some text in clipboard.
                        if (clipText != null) {

                            // Update only if the clip received is not in the clipboard already.
                            if (!oldClipText.contentEquals(clipText)) {

                                String content = clipText.toString();
                                try {
                                    FireClipUtils.sendTextNotification(ClipboardService.this, content, deviceName);
                                    FireClipUtils.setClipValue(content, deviceName);
                                } catch (JSONException e) {
                                    Toast.makeText(ClipboardService.this, "Copy failed!", Toast.LENGTH_SHORT).show();
                                }

                                oldClipText = content;

                                // Update local history any way. Act as local clipboard manager.
                                AndroidUtils.addHistoryItem(oldClipText, deviceName, new Date().getTime());

                            }

                        }

                    }

                }

            }
        };

        clipboardManager.addPrimaryClipChangedListener(clipChangedListener);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Get the current text from the clipboard and
     * store it onto oldClipText variable.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if ((clipboardManager.hasPrimaryClip()) &&
                (clipboardManager.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN))) {
            clipItem = clipboardManager.getPrimaryClip().getItemAt(0);
            oldClipText = clipItem.getText().toString();
        }

        return START_STICKY;
    }

    /**
     * Cleanup when destroy.
     */
    @Override
    public void onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(clipChangedListener);
        notificationManager.cancel(NotificationService.NOTIF_CLIP_ID);
        notificationManager.cancel(NotificationService.NOTIF_FILE_ID);
        super.onDestroy();
    }
}
