package com.abara.fireclip;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.abara.fireclip.util.FireClipUtils;

/**
 * Activity decides whether to show MainActivity or IntroActivity.
 * <p>
 * Created by abara on 29/07/16.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FireClipUtils.isUserSignedIn()) {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, IntroActivity.class));
        }
        finish();

    }

}
