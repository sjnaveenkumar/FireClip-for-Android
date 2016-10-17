package com.abara.fireclip.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.abara.fireclip.HistoryActivity;
import com.abara.fireclip.R;
import com.abara.fireclip.adapter.RecentHistoryAdapter;
import com.abara.fireclip.util.HistoryClip;
import com.abara.fireclip.util.ItemClickListener;
import com.abara.fireclip.util.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jaredrummler.android.device.DeviceName;

import java.util.Map;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by abara on 08/09/16.
 */

public class HistoryFragment extends Fragment {

    private static final String TAG = HistoryFragment.class.getSimpleName();
    private RecyclerView historyList;
    private Realm realm;

    private View historyEmptyView;
    private SharedPreferences prefs;

    private RealmChangeListener changeListener;
    private RecentHistoryAdapter historyAdapter;
    private RealmResults<HistoryClip> historyClipRealmResults;

    private DatabaseReference favRef;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);
        historyList = (RecyclerView) v.findViewById(R.id.history_recent_list);
        historyEmptyView = v.findViewById(R.id.history_empty_view);
        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        historyList.setItemAnimator(new DefaultItemAnimator());
        historyList.setLayoutManager(new LinearLayoutManager(getContext()));
        historyList.setHasFixedSize(false);

        realm = Realm.getDefaultInstance();
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        favRef = FirebaseDatabase.getInstance().getReference("users").child(firebaseUser.getUid()).child("fav");

        historyClipRealmResults = realm.where(HistoryClip.class).findAllSorted("timestamp", Sort.DESCENDING);
        historyAdapter = new RecentHistoryAdapter(getActivity(), historyClipRealmResults, new ItemClickListener() {
            @Override
            public void onItemClick(int position) {
                startActivity(new Intent(getActivity(), HistoryActivity.class));
            }

            @Override
            public void onLongClick(final int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.title_delete_dialog);
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        realm.beginTransaction();
                        historyClipRealmResults.deleteFromRealm(position);
                        realm.commitTransaction();
                        historyAdapter.notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }

            @Override
            public void onFavouriteClick(String content, String from, long timestamp) {
                // add to favourites
                Snackbar.make(getActivity().findViewById(R.id.history_recent_rootview), "Adding to favourites...", Snackbar.LENGTH_SHORT).show();
                DatabaseReference newFavRef = favRef.push();
                String key = newFavRef.getKey();
                String deviceName = prefs.getString(Utils.DEVICE_NAME_KEY, DeviceName.getDeviceName());
                Map<String, Object> favMap = Utils.generateFavMapClip(content, deviceName, timestamp, key);

                newFavRef.setValue(favMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Snackbar.make(getActivity().findViewById(R.id.history_recent_rootview), "Added to favourites", Snackbar.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(getActivity().findViewById(R.id.history_recent_rootview), "Couldn't add to favourites", Snackbar.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                });
            }


        });

        initOrUpdateAdapter();

        changeListener = new RealmChangeListener() {
            @Override
            public void onChange(Object element) {
                historyAdapter.notifyDataSetChanged();
                initOrUpdateAdapter();
            }
        };
        realm.addChangeListener(changeListener);
    }

    private void initOrUpdateAdapter() {
        RealmResults<HistoryClip> historyClipRealmResults = realm.where(HistoryClip.class).findAllSorted("timestamp", Sort.DESCENDING);
        if (historyClipRealmResults.size() != 0) {
            historyEmptyView.setVisibility(View.GONE);
            historyList.setAdapter(historyAdapter);
        } else {
            historyEmptyView.setVisibility(View.VISIBLE);
            historyList.setAdapter(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        initOrUpdateAdapter();
    }
}
