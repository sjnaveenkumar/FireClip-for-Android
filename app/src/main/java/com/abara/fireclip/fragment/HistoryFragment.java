package com.abara.fireclip.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.abara.fireclip.HistoryActivity;
import com.abara.fireclip.R;
import com.abara.fireclip.adapter.RecentHistoryAdapter;
import com.abara.fireclip.util.AndroidUtils;
import com.abara.fireclip.util.FireClipUtils;
import com.abara.fireclip.util.HistoryClip;
import com.abara.fireclip.util.ItemClickListener;
import com.google.android.gms.tasks.OnFailureListener;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * <p>Fragment to show Recent History items.</p>
 * <p>
 * Created by abara on 08/09/16.
 */
public class HistoryFragment extends Fragment implements ItemClickListener {

    private RecyclerView historyList;
    private Realm realm;

    private View historyEmptyView;
    private Context context;

    private RealmChangeListener changeListener;
    private RecentHistoryAdapter historyAdapter;
    private RealmResults<HistoryClip> historyClipRealmResults;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        historyList = (RecyclerView) view.findViewById(R.id.history_recent_list);
        historyEmptyView = view.findViewById(R.id.history_empty_view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        historyList.setItemAnimator(new DefaultItemAnimator());
        historyList.setLayoutManager(new LinearLayoutManager(getContext()));
        historyList.setHasFixedSize(false);

        realm = Realm.getDefaultInstance();

        historyClipRealmResults = realm.where(HistoryClip.class).findAllSorted("timestamp", Sort.DESCENDING);
        historyAdapter = new RecentHistoryAdapter(getActivity(), historyClipRealmResults, this);

        // Reload the list if an item is deleted or added to history.
        changeListener = new RealmChangeListener() {
            @Override
            public void onChange(Object element) {
                historyAdapter.notifyDataSetChanged();
            }
        };
        realm.addChangeListener(changeListener);

    }

    /**
     * Initialize or reload the list items.
     */
    private void initOrUpdateAdapter() {

        historyClipRealmResults = realm.where(HistoryClip.class).findAllSorted("timestamp", Sort.DESCENDING);
        if (historyClipRealmResults.size() != 0) {
            historyEmptyView.setVisibility(View.GONE);
            historyList.setAdapter(historyAdapter);
        } else {
            historyEmptyView.setVisibility(View.VISIBLE);
            historyList.setAdapter(null);
        }

    }

    /**
     * Calling initOrUpdateAdapter() here will keep the list always updated,
     * when user is navigating between activities.
     */
    @Override
    public void onResume() {
        super.onResume();
        initOrUpdateAdapter();
    }

    /**
     * Called when an HistoryItem is clicked.
     *
     * @param position position of item in the list.
     */
    @Override
    public void onItemClick(int position) {
        startActivity(new Intent(getActivity(), HistoryActivity.class));
    }

    /**
     * Called when an HistoryItem is long clicked.
     *
     * @param position position of item in the list.
     */
    @Override
    public void onLongClick(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.title_delete_dialog);
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                realm.beginTransaction();
                historyClipRealmResults.deleteFromRealm(position - 1);
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

    /**
     * Called when <b>PIN IT</b> is clicked.
     *
     * @param item HistoryItem to be pinned.
     */
    @Override
    public void onAddPin(final HistoryClip item) {
        Snackbar.make(getActivity().findViewById(R.id.history_recent_rootview), "Pinning...", Snackbar.LENGTH_SHORT).show();

        String deviceName = AndroidUtils.getDeviceName(getActivity());

        FireClipUtils.pinItem(item.getContent(), deviceName, item.getTimestamp())
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Couldn't pin!", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    /**
     * Remove the changeListener and close the realm instance.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (realm != null && !realm.isClosed()) {
            realm.removeChangeListener(changeListener);
            realm.close();
        }
    }

}
