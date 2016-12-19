package com.abara.fireclip;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;

import com.abara.fireclip.util.AndroidUtils;
import com.abara.fireclip.util.FireClipUtils;

import java.util.Date;

/**
 * Activity to get custom device name.
 * <p>
 * Created by abara on 25/09/16.
 */
public class DeviceNameActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String EXTRA_NEW_USER = "new";

    private TextInputEditText deviceNameBox;
    private AppCompatButton nextButton;

    private String deviceName;

    /**
     * Starter intent for this activity.
     */
    public static Intent getStarterIntent(Context context, boolean newUser) {
        Intent starterIntent = new Intent(context, DeviceNameActivity.class)
                .putExtra(EXTRA_NEW_USER, newUser);
        starterIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        return starterIntent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_name);

        nextButton = (AppCompatButton) findViewById(R.id.device_name_next_btn);
        deviceNameBox = (TextInputEditText) findViewById(R.id.device_name_input_box);

        deviceName = AndroidUtils.getDeviceName(this);
        deviceNameBox.setHint(deviceName);

        nextButton.setOnClickListener(this);

    }

    /**
     * Get the new device name.
     * Else assign the default device name.
     */
    @Override
    public void onClick(View view) {

        String name = deviceNameBox.getText().toString();

        if (!name.isEmpty()) {
            deviceName = name;
            AndroidUtils.setDeviceName(this, deviceName);
        }

        /**
         * Add an example pin and history for new users.
         */
        if (getIntent().getBooleanExtra(EXTRA_NEW_USER, false)) {
            long timestamp = new Date().getTime();
            FireClipUtils.pinItem(getResources().getString(R.string.pin_new_users),
                    deviceName, timestamp);
            AndroidUtils.addHistoryItem(getResources().getString(R.string.history_new_users), deviceName, timestamp);
        }

        Intent main = new Intent(DeviceNameActivity.this, MainActivity.class);
        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(main);

    }
}
