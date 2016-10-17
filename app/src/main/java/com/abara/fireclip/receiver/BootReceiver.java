package com.abara.fireclip.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.abara.fireclip.service.ClipboardService;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by abara on 23/09/16.
 */

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            context.startService(new Intent(context, ClipboardService.class));
        }

    }
}
