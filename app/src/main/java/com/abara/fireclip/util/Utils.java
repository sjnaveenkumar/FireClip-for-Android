package com.abara.fireclip.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.firebase.database.ServerValue;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.realm.Realm;

/**
 * Created by abara on 31/07/16.
 */
public class Utils {

    // Preference keys
    public static final String ENABLE_SERVICE_KEY = "enable_service";
    public static final String AUTO_ACCEPT_KEY = "auto_accept";
    public static final String DEVICE_NAME_KEY = "device_name";
    public static final String REM_MANUAL_HIS_KEY = "rem_manual_history";
    public static final String INITIAL_CARD_KEY = "initial_card";
    public static final String SILENT_NOTIF_KEY = "silent_notif";
    public static final String AUTO_ACCEPT_FILE_KEY = "auto_accept_file";
    public static final String LAST_DOWNLOAD_URL_KEY = "last_download_url";

    // FireClip datamap keys
    public static final String DATA_MAP_CONTENT = "content";
    public static final String DATA_MAP_FROM = "from";
    public static final String DATA_MAP_TIME = "timestamp";
    public static final String DATA_MAP_TIME_FAV = "timestamp_fav";
    public static final String DATA_MAP_KEY_FAV = "key_fav";
    public static final String DATA_MAP_FEED = "feedback";
    public static final String DATA_MAP_FILENAME = "filename";

    /*
    * Method to insert a new clip into local database.
    * */
    public static void updateRealmDB(String clipboardText, String deviceName, long timestamp) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        HistoryClip clip = new HistoryClip(clipboardText, timestamp, deviceName);
        realm.copyToRealm(clip);
        realm.commitTransaction();
        realm.close();
    }

    /*
    * Method to generate Map object.
    * */
    public static Map<String, Object> generateMapClip(String content, String deviceName) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(Utils.DATA_MAP_CONTENT, content);
        dataMap.put(Utils.DATA_MAP_FROM, deviceName);
        dataMap.put(Utils.DATA_MAP_TIME, ServerValue.TIMESTAMP);
        return dataMap;
    }

    /*
    * Method to generate Map object for new favourite clip items.
    * */
    public static Map<String, Object> generateFavMapClip(String content, String deviceName, long timestamp, String key) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(Utils.DATA_MAP_CONTENT, content);
        dataMap.put(Utils.DATA_MAP_FROM, deviceName);
        dataMap.put(Utils.DATA_MAP_TIME, timestamp);
        dataMap.put(Utils.DATA_MAP_KEY_FAV, key);
        dataMap.put(Utils.DATA_MAP_TIME_FAV, ServerValue.TIMESTAMP);
        return dataMap;
    }

    /*
    * Method to generate Map object for Files.
    * */
    public static Map<String, Object> generateMapClip(String s, String deviceName, String fileName) {
        Map<String, Object> dataMap = generateMapClip(s, deviceName);
        dataMap.put(DATA_MAP_FILENAME, fileName);
        return dataMap;
    }

    /*
    * Method to get updated time from since timestamp.
    * */
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

    /*
    * Method to check if device is online.
    * */
    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

}
