package com.abara.fireclip.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import android.widget.Toast;

import com.abara.fireclip.R;
import com.abara.fireclip.service.ClipboardService;
import com.abara.fireclip.util.AndroidUtils;
import com.abara.fireclip.util.FireClipUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;

/**
 * Fragment containing basic intro cards, manual update card, quick settings
 * and current clipboard details.
 * <p>
 * Created by abara on 08/09/16.
 */
public class HomeFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final String TAG = HomeFragment.class.getSimpleName();

    private CardView initialCard;
    private AppCompatButton gotItButton, updateButton;
    private AppCompatTextView initialCardTitleText, initialCardDescText;
    private TextInputEditText manualUpdateContentBox;
    private SwitchCompat enableServiceSwitch, autoAcceptSwitch;

    private FirebaseUser clipUser;

    private SharedPreferences preferences;
    private Context context;

    /**
     * Attach the context.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        preferences = AndroidUtils.getPreference(getActivity());
        clipUser = FirebaseAuth.getInstance().getCurrentUser();
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        // Initialize manual update card's components.
        manualUpdateContentBox = (TextInputEditText) view.findViewById(R.id.manual_update_card_content_box);
        updateButton = (AppCompatButton) view.findViewById(R.id.manual_update_card_button);

        //Initialize quick settings switch.
        enableServiceSwitch = (SwitchCompat) view.findViewById(R.id.quick_settings_card_1_switch);
        autoAcceptSwitch = (SwitchCompat) view.findViewById(R.id.quick_settings_card_2_switch);

        boolean shouldShowInitialCard = preferences.getBoolean(AndroidUtils.PREF_INITIAL_CARD, true);
        if (shouldShowInitialCard) {
            // Initialize initial card's components.
            initialCard = (CardView) view.findViewById(R.id.home_initial_card);
            gotItButton = (AppCompatButton) view.findViewById(R.id.initial_card_got_it_button);
            initialCardTitleText = (AppCompatTextView) view.findViewById(R.id.initial_card_title_text);
            initialCardDescText = (AppCompatTextView) view.findViewById(R.id.initial_card_desc_text);

            initialCard.setVisibility(View.VISIBLE);

            String deviceName = AndroidUtils.getDeviceName(getActivity());

            gotItButton.setOnClickListener(this);
            initialCardTitleText.setText(getResources().getString(R.string.initial_card_title, clipUser.getDisplayName()));
            initialCardDescText.setText(getResources().getString(R.string.initial_card_desc, deviceName));
        }

        updateButton.setOnClickListener(this);
        enableServiceSwitch.setOnCheckedChangeListener(this);
        autoAcceptSwitch.setOnCheckedChangeListener(this);

    }

    /**
     * Load preference values for quick settings here,
     * to reflect the changes from SettingsActivity.
     */
    @Override
    public void onStart() {
        super.onStart();
        boolean enableService = preferences.getBoolean(AndroidUtils.PREF_ENABLE_SERVICE, true);
        enableServiceSwitch.setChecked(enableService);

        boolean autoAcceptClip = preferences.getBoolean(AndroidUtils.PREF_AUTO_ACCEPT, false);
        autoAcceptSwitch.setChecked(autoAcceptClip);
    }

    /**
     * Handle click events.
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.initial_card_got_it_button:
                initialCard.setVisibility(View.GONE);
                preferences.edit().putBoolean(AndroidUtils.PREF_INITIAL_CARD, false).apply();
                break;
            case R.id.manual_update_card_button:
                updateContentToFirebase();
                break;
        }

    }

    /**
     * Update the content to FireClip database.
     */
    private void updateContentToFirebase() {

        final String content = manualUpdateContentBox.getText().toString();

        if (!content.isEmpty()) {

            final String deviceName = AndroidUtils.getDeviceName(getActivity());

            Snackbar.make(getView(), "Updating...", Snackbar.LENGTH_SHORT).show();

            final boolean shouldRemHistory = preferences.getBoolean(AndroidUtils.PREF_REM_MANUAL_HIS, false);

            FireClipUtils.setClipValue(content, deviceName).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Toast.makeText(context, "Content updated!", Toast.LENGTH_SHORT).show();
                    manualUpdateContentBox.setText("");
                    if (shouldRemHistory)
                        AndroidUtils.addHistoryItem(content, deviceName, new Date().getTime());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(context, "Content failed to update!", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "onFailure: Reason: " + e.getMessage());
                }
            });
        }

    }

    /**
     * Save settings and enable (or) disable service.
     */
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {

        switch (compoundButton.getId()) {
            case R.id.quick_settings_card_1_switch:
                preferences.edit().putBoolean(AndroidUtils.PREF_ENABLE_SERVICE, checked).apply();
                if (checked) {
                    getActivity().startService(new Intent(getActivity(), ClipboardService.class));
                } else {
                    getActivity().stopService(new Intent(getActivity(), ClipboardService.class));
                }
                autoAcceptSwitch.setEnabled(checked);
                break;
            case R.id.quick_settings_card_2_switch:
                preferences.edit().putBoolean(AndroidUtils.PREF_AUTO_ACCEPT, checked).apply();
                break;
        }

    }
}
