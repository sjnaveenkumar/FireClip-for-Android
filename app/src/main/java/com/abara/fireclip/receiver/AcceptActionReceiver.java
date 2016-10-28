package com.abara.fireclip.receiver;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;

import com.abara.fireclip.service.ClipboardService;
import com.abara.fireclip.util.Utils;

import java.util.Date;


/**
 * Created by abara on 25/09/16.
 */

/*
* BroadcastReceiver to accept the clips from notification.
* */
public class AcceptActionReceiver extends BroadcastReceiver {

    public static final String TEXT_EXTRA = "clip_text";
    public static final String FROM_EXTRA = "clip_from";
    public static final String CLEAR_NOTIF_EXTRA = "clear_notif";
    public static final String TIMESTAMP_EXTRA = "clip_timestamp";

    private ClipboardManager clipboardManager;

    @Override
    public void onReceive(Context context, Intent intent) {

        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        String text = intent.getStringExtra(TEXT_EXTRA);
        String from = intent.getStringExtra(FROM_EXTRA);
        Long timestamp = intent.getLongExtra(TIMESTAMP_EXTRA, new Date().getTime());

        ClipData data = ClipData.newPlainText("FireClipText", text);
        clipboardManager.setPrimaryClip(data);

        // Update the clip to local database as well.
        Utils.updateRealmDB(text, from, timestamp);

        if (intent.getBooleanExtra(CLEAR_NOTIF_EXTRA, false)) {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.cancel(ClipboardService.NEW_CLIP_NOTIFICATION_ID);
        }

    }
}
