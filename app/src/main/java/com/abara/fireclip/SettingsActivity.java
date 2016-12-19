package com.abara.fireclip;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.abara.fireclip.util.AndroidUtils;
import com.abara.fireclip.util.FireClipUtils;
import com.abara.fireclip.util.HistoryClip;
import com.jaredrummler.android.device.DeviceName;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Settings activity.
 * <p>
 * Created by abara on 14/10/16.
 */

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private LinearLayout enableLayout, autoAcceptLayout, remManualLayout, deviceNameLayout, silentNotifLayout, autoAcceptFileLayout;
    private SwitchCompat enableSwitch, autoAcceptSwitch, remManualSwitch, silentNotifSwitch, autoAcceptFileSwitch;
    private AppCompatTextView deviceNameText;

    private SharedPreferences prefs;
    private Realm realm;

    /**
     * Initialize components
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.settings_toolbar);
        toolbar.setTitle(R.string.settings_text);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = AndroidUtils.getPreference(this);
        realm = Realm.getDefaultInstance();

        enableLayout = (LinearLayout) findViewById(R.id.settings_enable_layout);
        autoAcceptLayout = (LinearLayout) findViewById(R.id.settings_auto_accept_layout);
        remManualLayout = (LinearLayout) findViewById(R.id.settings_remember_his_layout);
        deviceNameLayout = (LinearLayout) findViewById(R.id.settings_device_name_layout);
        silentNotifLayout = (LinearLayout) findViewById(R.id.settings_silent_notif_layout);
        autoAcceptFileLayout = (LinearLayout) findViewById(R.id.settings_auto_accept_file_layout);

        enableSwitch = (SwitchCompat) findViewById(R.id.settings_enable_switch);
        autoAcceptSwitch = (SwitchCompat) findViewById(R.id.settings_auto_accept_switch);
        remManualSwitch = (SwitchCompat) findViewById(R.id.settings_remember_his_switch);
        silentNotifSwitch = (SwitchCompat) findViewById(R.id.settings_silent_notif_switch);
        autoAcceptFileSwitch = (SwitchCompat) findViewById(R.id.settings_auto_accept_file_switch);

        deviceNameText = (AppCompatTextView) findViewById(R.id.settings_device_name_title);

        enableLayout.setOnClickListener(this);
        autoAcceptLayout.setOnClickListener(this);
        remManualLayout.setOnClickListener(this);
        deviceNameLayout.setOnClickListener(this);
        silentNotifLayout.setOnClickListener(this);
        autoAcceptFileLayout.setOnClickListener(this);

        loadAndSetupComponents();

        enableSwitch.setOnCheckedChangeListener(this);
        autoAcceptSwitch.setOnCheckedChangeListener(this);
        remManualSwitch.setOnCheckedChangeListener(this);
        silentNotifSwitch.setOnCheckedChangeListener(this);
        autoAcceptFileSwitch.setOnCheckedChangeListener(this);

    }

    /**
     * Load preference values into components.
     */
    private void loadAndSetupComponents() {
        boolean enableService = prefs.getBoolean(AndroidUtils.PREF_ENABLE_SERVICE, true);
        enableSwitch.setChecked(enableService);

        boolean autoAccept = prefs.getBoolean(AndroidUtils.PREF_AUTO_ACCEPT, false);
        autoAcceptSwitch.setChecked(autoAccept);
        autoAcceptSwitch.setEnabled(enableService);
        autoAcceptLayout.setClickable(enableService);

        boolean autoAcceptFiles = prefs.getBoolean(AndroidUtils.PREF_AUTO_ACCEPT_FILE, false);
        autoAcceptFileSwitch.setChecked(autoAcceptFiles);
        autoAcceptFileSwitch.setEnabled(enableService);
        autoAcceptFileSwitch.setClickable(enableService);

        boolean silentNotif = prefs.getBoolean(AndroidUtils.PREF_SILENT_NOTIF, false);
        silentNotifSwitch.setChecked(silentNotif);

        boolean remManualHis = prefs.getBoolean(AndroidUtils.PREF_REM_MANUAL_HIS, false);
        remManualSwitch.setChecked(remManualHis);

        String deviceName = prefs.getString(AndroidUtils.PREF_DEVICE_NAME, DeviceName.getDeviceName());
        deviceNameText.setText(deviceName);
    }

    /**
     * Go back, on pressing close button.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle switch toggles.
     */
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
            case R.id.settings_silent_notif_layout:
                toogleSwitch(silentNotifSwitch);
                break;
            case R.id.settings_auto_accept_file_layout:
                toogleSwitch(autoAcceptFileSwitch);
        }

    }

    /**
     * Toggle switch values.
     *
     * @param s The switch item to be toggled.
     */
    private void toogleSwitch(SwitchCompat s) {
        boolean isChecked = s.isChecked();
        s.setChecked(!isChecked);
    }

    /**
     * Handle checked behaviour of the switch.
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        String key = AndroidUtils.PREF_ENABLE_SERVICE;
        int id = buttonView.getId();

        switch (id) {
            case R.id.settings_enable_switch:
                key = AndroidUtils.PREF_ENABLE_SERVICE;
                boolean checked = enableSwitch.isChecked();
                if (checked) {
                    startService(new Intent(this, ClipboardService.class));
                    FireClipUtils.subscribe();
                } else {
                    stopService(new Intent(this, ClipboardService.class));
                    FireClipUtils.unSubscribe();
                }
                autoAcceptSwitch.setEnabled(checked);
                autoAcceptLayout.setClickable(checked);
                autoAcceptFileSwitch.setEnabled(checked);
                autoAcceptFileLayout.setClickable(checked);
                break;
            case R.id.settings_auto_accept_switch:
                key = AndroidUtils.PREF_AUTO_ACCEPT;
                break;
            case R.id.settings_remember_his_switch:
                key = AndroidUtils.PREF_REM_MANUAL_HIS;
                break;
            case R.id.settings_silent_notif_switch:
                key = AndroidUtils.PREF_SILENT_NOTIF;
                break;
            case R.id.settings_auto_accept_file_switch:
                key = AndroidUtils.PREF_AUTO_ACCEPT_FILE;
                break;
        }

        // Commit to make changes immediatly.
        prefs.edit().putBoolean(key, isChecked).commit();

    }

    /**
     * Dialog to change device name.
     */
    private void showDeviceNameDialog() {

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_device_name, null);
        final TextInputEditText deviceNameBox = (TextInputEditText) view.findViewById(R.id.dialog_device_name_box);
        final String currName = AndroidUtils.getDeviceName(this);
        deviceNameBox.setHint(currName);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Device name")
                .setView(view)
                .setPositiveButton(R.string.action_change, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {

                        String newName = deviceNameBox.getText().toString();
                        if (!newName.isEmpty()) {
                            prefs.edit().putString(AndroidUtils.PREF_DEVICE_NAME, newName).commit();
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
                setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

}
