package com.abara.fireclip.receiver;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;

import com.abara.fireclip.service.NotificationService;
import com.abara.fireclip.util.AndroidUtils;

import java.util.Date;


/**
 *
 * BroadcastReceiver to accept the clips from notification.
 *
 * Created by abara on 25/09/16.
 */
public class AcceptClipActionReceiver extends BroadcastReceiver {

    public static final String EXTRA_CLEAR_NOTIFICATION = "clear";
    private static final String EXTRA_TEXT = "clip_text";
    private static final String EXTRA_FROM = "clip_from";
    private static final String EXTRA_TIMESTAMP = "clip_timestamp";

    private ClipboardManager clipboardManager;

    public static Intent getStarterIntent(Context context, String content, String from, String timestamp) {
        Intent starterIntent = new Intent(context, AcceptClipActionReceiver.class);
        starterIntent.putExtra(EXTRA_TEXT, content);
        starterIntent.putExtra(EXTRA_FROM, from);
        starterIntent.putExtra(EXTRA_TIMESTAMP, Long.parseLong(timestamp));
        return starterIntent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);

        String text = intent.getStringExtra(EXTRA_TEXT);
        String from = intent.getStringExtra(EXTRA_FROM);
        Long timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, new Date().getTime());

        ClipData data = ClipData.newPlainText("FireClipText", text);
        clipboardManager.setPrimaryClip(data);

        // Update the clip to local database as well.
        AndroidUtils.addHistoryItem(text, from, timestamp);

        if (intent.getBooleanExtra(EXTRA_CLEAR_NOTIFICATION, false)) {
            NotificationManagerCompat manager = NotificationManagerCompat.from(context);
            manager.cancel(NotificationService.NOTIF_CLIP_ID);
        }

    }
}
