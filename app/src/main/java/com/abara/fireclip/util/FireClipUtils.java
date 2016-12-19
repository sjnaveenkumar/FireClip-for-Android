package com.abara.fireclip.util;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.abara.fireclip.R;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * FireClip Utility class.
 * <p>
 * Created by abara on 04/12/16.
 */

public class FireClipUtils {

    // Push message keys.
    public static final String PUSH_TO_KEY = "to";
    public static final String PUSH_DATA_KEY = "data";
    public static final String PUSH_CONTENT_KEY = "content";
    public static final String PUSH_TIMESTAMP_KEY = "timestamp";
    public static final String PUSH_DEVICE_KEY = "device_name";
    public static final String PUSH_FILE_KEY = "is_file";
    public static final String PUSH_DOWNLOAD_URL_KEY = "url";
    public static final String PUSH_FILE_NAME_KEY = "file_name";
    public static final String TYPE_FILE = "1";
    public static final String TYPE_TEXT = "0";
    private static final String PUSH_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String PUSH_AUTH_HEADER = "Authorization";
    private static final String PUSH_CONTENT_HEADER = "Content-Type";

    // Database References and current user.
    private static final String REFERENCE_USERS = "users";
    private static final String REFERENCE_PINS = "fav";
    private static final String REFERENCE_CLIP = "clip";
    private static final String REFERENCE_FEEDBACK = "feedbacks";
    private static final String REFERENCE_FILE = "file";

    /**
     * Get the current FireClip user.
     *
     * @return the user.
     */
    private static FirebaseUser getUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    /**
     * Get the root reference.
     *
     * @return the database reference.
     */
    private static DatabaseReference getRootRef() {
        return FirebaseDatabase.getInstance().getReference();
    }

    /**
     * Get the user reference.
     *
     * @return the database reference.
     */
    private static DatabaseReference getUserRef() {
        return getRootRef().child(REFERENCE_USERS).child(getUser().getUid());
    }

    /**
     * Store the copied content onto FireClip database.
     *
     * @param content    The content that was copied.
     * @param deviceName Name of the device, from where the text was copied (current device).
     * @return the task.
     */
    public static Task<Void> setClipValue(String content, String deviceName) {

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(AndroidUtils.DATA_MAP_CONTENT, content);
        dataMap.put(AndroidUtils.DATA_MAP_FROM, deviceName);
        dataMap.put(AndroidUtils.DATA_MAP_TIME, ServerValue.TIMESTAMP);

        return getClipReference().setValue(dataMap);

    }

    /**
     * Pin an item (ie. text) to make it available for all of the user's devices.
     * Push the item onto FireClip database.
     *
     * @param content    The content that was pinned.
     * @param deviceName Name of the device, from where the text was pinned (current device).
     * @param timestamp  When the item was pinned.
     * @return the task.
     */
    public static Task<Void> pinItem(String content, String deviceName, long timestamp) {
        DatabaseReference newPinRef = getPinReference().push();
        String key = newPinRef.getKey();

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(AndroidUtils.DATA_MAP_CONTENT, content);
        dataMap.put(AndroidUtils.DATA_MAP_FROM, deviceName);
        dataMap.put(AndroidUtils.DATA_MAP_TIME, timestamp);
        dataMap.put(AndroidUtils.DATA_MAP_KEY_FAV, key);
        dataMap.put(AndroidUtils.DATA_MAP_TIME_FAV, ServerValue.TIMESTAMP);

        return newPinRef.setValue(dataMap);

    }

    /**
     * Unpin the text, that is no more needed by the user.
     * Remove the pinned item from FireClip database.
     *
     * @param key The Pinned item's key, that is to be removed.
     * @return the task.
     */
    public static Task<Void> unpinItem(String key) {
        return getPinReference().child(key).removeValue();
    }

    /**
     * Get the reference to user's pins.
     *
     * @return pin reference.
     */
    public static DatabaseReference getPinReference() {
        return getUserRef().child(REFERENCE_PINS);
    }

    /**
     * Get the reference to user's clip (ie. text).
     *
     * @return clip reference.
     */
    public static DatabaseReference getClipReference() {
        return getUserRef().child(REFERENCE_CLIP);
    }

    /**
     * Get the reference to feedbacks.
     *
     * @return feedback reference.
     */
    public static DatabaseReference getFeedbackReference() {
        return getRootRef().child(REFERENCE_FEEDBACK);
    }

    /**
     * Add a new feedback.
     *
     * @param feedback The feedback text.
     * @return feedback reference.
     */
    public static Task<Void> addFeedback(Context context, String feedback) {
        Map<String, Object> feedMap = new HashMap<>();
        feedMap.put(AndroidUtils.DATA_MAP_TIME, ServerValue.TIMESTAMP);
        feedMap.put(AndroidUtils.DATA_MAP_FEED, feedback);
        feedMap.put(AndroidUtils.DATA_MAP_FROM, AndroidUtils.getDeviceName(context));
        return getFeedbackReference().child(getUser().getUid()).push().setValue(feedMap);
    }

    /**
     * Check if user is signed in or not.
     *
     * @return true if signed in, else false.
     */
    public static boolean isUserSignedIn() {
        return (getUser() != null);
    }

    /**
     * Return the user's storage bucket reference.
     *
     * @return feedback reference.
     */
    public static StorageReference getFileStorageReference(String filename) {
        return FirebaseStorage.getInstance().getReference(REFERENCE_USERS).child(getUser().getUid()).child(filename);
    }

    /**
     * Get the reference to file.
     *
     * @return feedback reference.
     */
    public static DatabaseReference getFileReference() {
        return getUserRef().child(REFERENCE_FILE);
    }

    /**
     * Sync the file to all of the user's devices.
     *
     * @param downloadUrl The download url of the file.
     * @param fileName    Name of the file.
     * @return the task.
     */
    public static Task<Void> syncFile(Context context, Uri downloadUrl, String fileName) {

        String deviceName = AndroidUtils.getDeviceName(context);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(AndroidUtils.DATA_MAP_CONTENT, downloadUrl.toString());
        dataMap.put(AndroidUtils.DATA_MAP_FROM, deviceName);
        dataMap.put(AndroidUtils.DATA_MAP_TIME, ServerValue.TIMESTAMP);
        dataMap.put(AndroidUtils.DATA_MAP_FILENAME, fileName);

        return getFileReference().setValue(dataMap);
    }

    /**
     * Update the user's name and photo.
     *
     * @param name     Name of the user.
     * @param photoUrl Photo url of the user.
     * @return the task.
     */
    public static Task<Void> updateUserDetails(String name, Uri photoUrl) {
        UserProfileChangeRequest.Builder changeRequestBuilder = new UserProfileChangeRequest.Builder();
        if (name != null) changeRequestBuilder.setDisplayName(name);
        if (photoUrl != null) changeRequestBuilder.setPhotoUri(photoUrl);
        UserProfileChangeRequest changeRequest = changeRequestBuilder.build();
        return getUser().updateProfile(changeRequest);
    }

    /**
     * Send a push notification to notify all of the user's devices, whenever
     * new file is uploaded onto FireClip storage.
     *
     * @param downloadUrl The download url of the file.
     * @param fileName    Name of the file.
     */
    public static void sendFileNotification(Context context, Uri downloadUrl, String fileName)
            throws JSONException {

        JSONObject fileData = new JSONObject();
        fileData.put(PUSH_DOWNLOAD_URL_KEY, downloadUrl.toString())
                .put(PUSH_TIMESTAMP_KEY, String.valueOf(new Date().getTime()))
                .put(PUSH_DEVICE_KEY, AndroidUtils.getDeviceName(context))
                .put(PUSH_FILE_NAME_KEY, fileName)
                .put(PUSH_FILE_KEY, TYPE_FILE);

        sendNotification(context, fileData);

    }


    /**
     * Send a push notification to notify all of the user's devices, whenever
     * new text is copied onto clipboard.
     *
     * @param content    The content that was copied.
     * @param deviceName Name of the device, from where the text was copied (current device).
     */
    public static void sendTextNotification(final Context context, final String content, String deviceName) throws JSONException {

        JSONObject data = new JSONObject();
        data.put(PUSH_CONTENT_KEY, content)
                .put(PUSH_TIMESTAMP_KEY, String.valueOf(new Date().getTime()))
                .put(PUSH_DEVICE_KEY, deviceName)
                .put(PUSH_FILE_KEY, TYPE_TEXT);

        sendNotification(context, data);

    }

    /**
     * Send data message as push notification.
     *
     * @param data The data payload of the push message.
     */
    private static void sendNotification(final Context context, JSONObject data) throws JSONException {

        JSONObject reqData = new JSONObject();

        reqData.put(PUSH_TO_KEY, "/topics/" + getUser().getUid());
        reqData.put(PUSH_DATA_KEY, data);

        JsonObjectRequest notificationRequest = new JsonObjectRequest(Request.Method.POST, PUSH_URL,
                reqData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {
                    response.getLong("message_id");
                } catch (JSONException e) {
                    Toast.makeText(context, "Copy failed!", Toast.LENGTH_SHORT).show();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context, "Copy failed!", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {

                Map<String, String> headers = new HashMap<>();
                headers.put(PUSH_AUTH_HEADER, "key=" + context.getResources().getString(R.string.firebase_server_key));
                headers.put(PUSH_CONTENT_HEADER, "application/json");

                return headers;
            }
        };

        Volley.newRequestQueue(context).add(notificationRequest);

    }

    /**
     * Subscribe to user's UID to receive push notifications.
     */
    public static void subscribe() {
        FirebaseMessaging.getInstance().subscribeToTopic(getUser().getUid());
    }

    /**
     * UnSubscribe to user's UID to receive push notifications.
     */
    public static void unSubscribe() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(getUser().getUid());
    }

}
