package com.abara.fireclip.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.abara.fireclip.R;
import com.abara.fireclip.service.ClipboardService;
import com.abara.fireclip.util.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.jaredrummler.android.device.DeviceName;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by abara on 08/09/16.
 */

public class HomeFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = HomeFragment.class.getSimpleName();

    private CardView initialCard;
    private AppCompatButton gotItButton, updateButton;
    private AppCompatTextView initialCardTitleText, initialCardDescText;
    private TextInputEditText manualUpdateContentBox;
    private SwitchCompat enableServiceSwitch, autoAcceptSwitch;

    private DatabaseReference userClipRef;
    private FirebaseUser clipUser;

    private SharedPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        clipUser = FirebaseAuth.getInstance().getCurrentUser();
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        // Initialize manual update card's components
        manualUpdateContentBox = (TextInputEditText) view.findViewById(R.id.manual_update_card_content_box);
        updateButton = (AppCompatButton) view.findViewById(R.id.manual_update_card_button);

        //Initialize quick settings switch
        enableServiceSwitch = (SwitchCompat) view.findViewById(R.id.quick_settings_card_1_switch);
        autoAcceptSwitch = (SwitchCompat) view.findViewById(R.id.quick_settings_card_2_switch);

        boolean shouldShowInitialCard = preferences.getBoolean(Utils.INITIAL_CARD_KEY, true);

        if (shouldShowInitialCard) {
            // Initialize initial card's components
            initialCard = (CardView) view.findViewById(R.id.home_initial_card);
            gotItButton = (AppCompatButton) view.findViewById(R.id.initial_card_got_it_button);
            initialCardTitleText = (AppCompatTextView) view.findViewById(R.id.initial_card_title_text);
            initialCardDescText = (AppCompatTextView) view.findViewById(R.id.initial_card_desc_text);

            initialCard.setVisibility(View.VISIBLE);

            String deviceName = preferences.getString(Utils.DEVICE_NAME_KEY, DeviceName.getDeviceName());

            gotItButton.setOnClickListener(this);
            initialCardTitleText.setText(getResources().getString(R.string.initial_card_title, clipUser.getDisplayName()));
            initialCardDescText.setText(getResources().getString(R.string.initial_card_desc, deviceName));
        }

        updateButton.setOnClickListener(this);
        enableServiceSwitch.setOnCheckedChangeListener(this);
        autoAcceptSwitch.setOnCheckedChangeListener(this);

    }

    @Override
    public void onStart() {
        super.onStart();

        boolean enableService = preferences.getBoolean(Utils.ENABLE_SERVICE_KEY, true);
        enableServiceSwitch.setChecked(enableService);
        boolean autoAcceptClip = preferences.getBoolean(Utils.AUTO_ACCEPT_KEY, false);
        autoAcceptSwitch.setChecked(autoAcceptClip);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.initial_card_got_it_button:
                initialCard.setVisibility(View.GONE);
                preferences.edit().putBoolean(Utils.INITIAL_CARD_KEY, false).apply();
                break;
            case R.id.manual_update_card_button:
                updateContentToFirebase();
                break;
        }

    }

    private void updateContentToFirebase() {

        userClipRef = FirebaseDatabase.getInstance().getReference("users").child(clipUser.getUid()).child("clip");

        final String content = manualUpdateContentBox.getText().toString();

        if (!content.isEmpty()) {

            final String deviceName = preferences.getString(Utils.DEVICE_NAME_KEY, DeviceName.getDeviceName());

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put(Utils.DATA_MAP_CONTENT, content);
            dataMap.put(Utils.DATA_MAP_FROM, deviceName);
            dataMap.put(Utils.DATA_MAP_TIME, ServerValue.TIMESTAMP);

            Snackbar.make(getView(), "Updating...", Snackbar.LENGTH_SHORT).show();

            final boolean shouldRemHistory = preferences.getBoolean(Utils.REM_MANUAL_HIS_KEY, false);

            userClipRef.setValue(dataMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Snackbar.make(getView(), "Content updated!", Snackbar.LENGTH_SHORT).show();
                    manualUpdateContentBox.setText("");
                    if (shouldRemHistory)
                        Utils.updateRealmDB(content, deviceName, new Date().getTime());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Snackbar.make(getView(), "Content failed to update!", Snackbar.LENGTH_SHORT).show();
                    Log.e(TAG, "onFailure: Reason: " + e.getMessage());
                }
            });
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {

        switch (compoundButton.getId()) {
            case R.id.quick_settings_card_1_switch:
                preferences.edit().putBoolean(Utils.ENABLE_SERVICE_KEY, checked).commit();
                if (checked) {
                    getActivity().startService(new Intent(getActivity(), ClipboardService.class));
                } else {
                    getActivity().stopService(new Intent(getActivity(), ClipboardService.class));
                }
                autoAcceptSwitch.setEnabled(checked);
                break;
            case R.id.quick_settings_card_2_switch:
                preferences.edit().putBoolean(Utils.AUTO_ACCEPT_KEY, checked).commit();
                break;
        }

    }
}
