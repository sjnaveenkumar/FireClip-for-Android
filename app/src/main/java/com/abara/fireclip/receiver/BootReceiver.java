package com.abara.fireclip.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.abara.fireclip.service.ClipboardService;
import com.abara.fireclip.util.AndroidUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 *
 * BroadcastReceiver to decide whether to start the ClipboardService.
 *
 * Created by abara on 23/09/16.
 */
public class BootReceiver extends BroadcastReceiver {

    private SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        prefs = AndroidUtils.getPreference(context);

        boolean enableService = prefs.getBoolean(AndroidUtils.PREF_ENABLE_SERVICE, true);

        if ((user != null) && enableService) {
            context.startService(new Intent(context, ClipboardService.class));
        }

    }
}
