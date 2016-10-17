package com.abara.fireclip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.View;

import com.abara.fireclip.util.Utils;
import com.jaredrummler.android.device.DeviceName;

/**
 * Created by abara on 25/09/16.
 */

public class DeviceNameActivity extends AppCompatActivity implements View.OnClickListener {

    private TextInputEditText deviceNameBox;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_name);

        AppCompatButton nextButton = (AppCompatButton) findViewById(R.id.device_name_next_btn);
        deviceNameBox = (TextInputEditText) findViewById(R.id.device_name_input_box);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String deviceName = preferences.getString(Utils.DEVICE_NAME_KEY, DeviceName.getDeviceName());
        deviceNameBox.setHint(deviceName);

        nextButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {

        String deviceName = deviceNameBox.getText().toString();

        if (!deviceName.isEmpty()) {
            preferences.edit().putString(Utils.DEVICE_NAME_KEY, deviceName).apply();
        }

        Intent main = new Intent(DeviceNameActivity.this, MainActivity.class);
        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(main);

    }
}
