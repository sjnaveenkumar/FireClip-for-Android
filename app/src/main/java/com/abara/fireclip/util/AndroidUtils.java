package com.abara.fireclip.util;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import com.abara.fireclip.FeedbackActivity;
import com.abara.fireclip.R;
import com.jaredrummler.android.device.DeviceName;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;

/**
 * Android Utility class.
 * <p>
 * Created by abara on 31/07/16.
 */
public class AndroidUtils {

    /**
     * Preference keys.
     */
    public static final String PREF_ENABLE_SERVICE = "enable_service";
    public static final String PREF_AUTO_ACCEPT = "auto_accept";
    public static final String PREF_DEVICE_NAME = "device_name";
    public static final String PREF_REM_MANUAL_HIS = "rem_manual_history";
    public static final String PREF_INITIAL_CARD = "initial_card";
    public static final String PREF_SILENT_NOTIF = "silent_notif";
    public static final String PREF_AUTO_ACCEPT_FILE = "auto_accept_file";

    /**
     * FireClip datamap keys.
     */
    public static final String DATA_MAP_CONTENT = "content";
    public static final String DATA_MAP_FROM = "from";
    public static final String DATA_MAP_TIME = "timestamp";
    public static final String DATA_MAP_TIME_FAV = "timestamp_fav";
    public static final String DATA_MAP_KEY_FAV = "key_fav";
    public static final String DATA_MAP_FEED = "feedback";
    public static final String DATA_MAP_FILENAME = "filename";

    private static final int REQUEST_PERMISSION_ID = 9543;

    /**
     * Method to insert a new history item into realm database.
     *
     * @param content    Content of the history item.
     * @param deviceName Name of the device, the text received from.
     * @param timestamp  Time when the text received.
     */
    public static void addHistoryItem(String content, String deviceName, long timestamp) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        HistoryClip clip = new HistoryClip(content, timestamp, deviceName);
        realm.copyToRealm(clip);
        realm.commitTransaction();
        realm.close();
    }

    /**
     * Method to get updated time from since timestamp.
     *
     * @param timestamp The time when the history item was added.
     */
    public static String getTimeSince(long timestamp) {

        Date now = new Date();
        Date since = new Date(timestamp);

        long secondsPast = (now.getTime() - since.getTime()) / 1000;

        if (secondsPast < 60) {
            if (secondsPast < 10) {
                return "Now";
            } else {
                return "Few seconds";
            }
        }
        if (secondsPast < 3600) {
            if ((secondsPast / 60) == 1) {
                return "1 min";
            } else {
                return secondsPast / 60 + " mins";
            }
        }
        if (secondsPast <= 86400) {
            long hr = secondsPast / 3600;
            if (hr == 1) {
                return "1 hr";
            } else {
                return hr + " hrs";
            }
        } else {

            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);
            Calendar sinceCal = Calendar.getInstance();
            sinceCal.setTime(since);

            if (sinceCal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && sinceCal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
                return "Yesterday";
            } else {
                SimpleDateFormat format = new SimpleDateFormat("dd MMMM yy", Locale.ENGLISH);
                return format.format(since);
            }

        }

    }

    /**
     * Check if device is online.
     */
    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null) && networkInfo.isConnectedOrConnecting();
    }

    /**
     * Return the application's preference.
     */
    public static SharedPreferences getPreference(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    /**
     * Return the device name.
     */
    public static String getDeviceName(Context context) {
        return getPreference(context).getString(PREF_DEVICE_NAME, DeviceName.getDeviceName());
    }

    /**
     * Set the device name.
     */
    public static void setDeviceName(Context context, String deviceName) {
        getPreference(context).edit().putString(PREF_DEVICE_NAME, deviceName).apply();
    }

    /**
     * Set feedback action to notification.
     *
     * @param builder Notification's builder to add the Feedback form.
     */
    public static void addFeedbackAction(Context context, NotificationCompat.Builder builder) {
        Intent feedbackIntent = new Intent(context, FeedbackActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, feedbackIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ic_action_feedback, "Send Feedback", pi);
    }

    /**
     * Copy new text to the clipboard.
     *
     * @param text The text to be copied into clipboard.
     */
    public static void copyToClipboard(Context context, String text) {
        ClipData data = ClipData.newPlainText("FireClipText", text);
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        manager.setPrimaryClip(data);
    }

    public static void askStoragePermission(Activity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Ask for permission to read and write external storage.
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Toast.makeText(context, "This permission is needed to receive files from your devices.", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(context,
                            new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION_ID);
                } else {
                    ActivityCompat.requestPermissions(context,
                            new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION_ID);
                }

            }

        }
    }
}
