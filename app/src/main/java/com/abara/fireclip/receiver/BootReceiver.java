package com.abara.fireclip.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.abara.fireclip.service.ClipboardService;
import com.abara.fireclip.util.Utils;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by abara on 23/09/16.
 */

/*
* Start service after device is booted.
* */
public class BootReceiver extends BroadcastReceiver {

    private SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());

        boolean enableService = prefs.getBoolean(Utils.ENABLE_SERVICE_KEY, true);

        if ((firebaseAuth.getCurrentUser() != null) && enableService) {
            context.startService(new Intent(context, ClipboardService.class));
        }

    }
}
