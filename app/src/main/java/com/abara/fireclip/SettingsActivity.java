package com.abara.fireclip;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.abara.fireclip.service.ClipboardService;
import com.abara.fireclip.util.HistoryClip;
import com.abara.fireclip.util.Utils;
import com.jaredrummler.android.device.DeviceName;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by abara on 14/10/16.
 */

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private LinearLayout enableLayout, autoAcceptLayout, remManualLayout, deviceNameLayout;
    private SwitchCompat enableSwitch, autoAcceptSwitch, remManualSwitch;
    private AppCompatTextView deviceNameText;

    private SharedPreferences prefs;
    private Realm realm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle(R.string.settings_text);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        realm = Realm.getDefaultInstance();

        enableLayout = (LinearLayout) findViewById(R.id.settings_enable_layout);
        autoAcceptLayout = (LinearLayout) findViewById(R.id.settings_auto_accept_layout);
        remManualLayout = (LinearLayout) findViewById(R.id.settings_remember_his_layout);
        deviceNameLayout = (LinearLayout) findViewById(R.id.settings_device_name_layout);

        enableSwitch = (SwitchCompat) findViewById(R.id.settings_enable_switch);
        autoAcceptSwitch = (SwitchCompat) findViewById(R.id.settings_auto_accept_switch);
        remManualSwitch = (SwitchCompat) findViewById(R.id.settings_remember_his_switch);

        deviceNameText = (AppCompatTextView) findViewById(R.id.settings_device_name_title);

        enableLayout.setOnClickListener(this);
        autoAcceptLayout.setOnClickListener(this);
        remManualLayout.setOnClickListener(this);
        deviceNameLayout.setOnClickListener(this);

        boolean enableService = prefs.getBoolean(Utils.ENABLE_SERVICE_KEY, true);
        enableSwitch.setChecked(enableService);
        boolean autoAccept = prefs.getBoolean(Utils.AUTO_ACCEPT_KEY, false);
        autoAcceptSwitch.setChecked(autoAccept);
        autoAcceptSwitch.setEnabled(enableService);
        autoAcceptLayout.setClickable(enableService);
        boolean remManualHis = prefs.getBoolean(Utils.REM_MANUAL_HIS_KEY, false);
        remManualSwitch.setChecked(remManualHis);
        String deviceName = prefs.getString(Utils.DEVICE_NAME_KEY, DeviceName.getDeviceName());
        deviceNameText.setText(deviceName);

        enableSwitch.setOnCheckedChangeListener(this);
        autoAcceptSwitch.setOnCheckedChangeListener(this);
        remManualSwitch.setOnCheckedChangeListener(this);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.settings_enable_layout:
                toogleSwitch(enableSwitch);
                break;
            case R.id.settings_auto_accept_layout:
                toogleSwitch(autoAcceptSwitch);
                break;
            case R.id.settings_remember_his_layout:
                toogleSwitch(remManualSwitch);
                break;
            case R.id.settings_device_name_layout:
                showDeviceNameDialog();
                break;
        }

    }

    private void toogleSwitch(SwitchCompat aSwitch) {
        boolean isChecked = aSwitch.isChecked();
        aSwitch.setChecked(!isChecked);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        String key = Utils.ENABLE_SERVICE_KEY;
        int id = buttonView.getId();

        switch (id) {
            case R.id.settings_enable_switch:
                key = Utils.ENABLE_SERVICE_KEY;
                boolean checked = enableSwitch.isChecked();
                if (checked) {
                    startService(new Intent(this, ClipboardService.class));
                } else {
                    stopService(new Intent(this, ClipboardService.class));
                }
                autoAcceptSwitch.setEnabled(checked);
                autoAcceptLayout.setClickable(checked);
                break;
            case R.id.settings_auto_accept_switch:
                key = Utils.AUTO_ACCEPT_KEY;
                break;
            case R.id.settings_remember_his_switch:
                key = Utils.REM_MANUAL_HIS_KEY;
                break;
        }

        prefs.edit().putBoolean(key, isChecked).commit();

    }

    private void showDeviceNameDialog() {

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_device_name, null);
        final TextInputEditText deviceNameBox = (TextInputEditText) view.findViewById(R.id.dialog_device_name_box);
        final String currName = prefs.getString(Utils.DEVICE_NAME_KEY, DeviceName.getDeviceName());
        deviceNameBox.setHint(currName);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Device name")
                .setView(view)
                .setPositiveButton("Change", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {

                        String newName = deviceNameBox.getText().toString();
                        if (!newName.isEmpty()) {
                            prefs.edit().putString(Utils.DEVICE_NAME_KEY, newName).commit();
                            Toast.makeText(SettingsActivity.this, "Device name changed!", Toast.LENGTH_SHORT).show();
                            deviceNameText.setText(newName);
                            RealmResults<HistoryClip> historyClips = realm.where(HistoryClip.class).equalTo("from", currName).findAll();
                            realm.beginTransaction();
                            for (HistoryClip historyClip : historyClips) {
                                historyClip.setFrom(newName);
                            }
                            realm.commitTransaction();
                        }

                    }
                }).
                setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

}
