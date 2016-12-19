package com.abara.fireclip.fragment;


import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.abara.fireclip.R;
import com.abara.fireclip.adapter.PinAdapter;
import com.abara.fireclip.adapter.PinHolder;
import com.abara.fireclip.util.FireClipUtils;
import com.abara.fireclip.util.PinItem;
import com.abara.fireclip.util.PinItemClickListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

/**
 * Fragment containing PinItems.
 * <p>
 * Created by abara on 09/10/16.
 */
public class PinsFragment extends Fragment implements PinItemClickListener {

    private static final String TAG = PinsFragment.class.getSimpleName();
    private DatabaseReference pinRef;

    private RecyclerView pinsList;
    private AppCompatTextView loadingText;
    private ProgressBar progressbar;
    private Context context;

    private PinAdapter adapter;
    private RecyclerView.AdapterDataObserver observer;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favourites, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        loadingText = (AppCompatTextView) view.findViewById(R.id.home_fav_empty_text);
        progressbar = (ProgressBar) view.findViewById(R.id.home_fav_progress);
        pinsList = (RecyclerView) view.findViewById(R.id.home_favorites_list);

        pinsList.setHasFixedSize(false);
        pinsList.setItemAnimator(new DefaultItemAnimator());

        // Reverse pins, so that latest appears at top.
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        pinsList.setLayoutManager(linearLayoutManager);

        pinRef = FireClipUtils.getPinReference();
        pinRef.keepSynced(true);

        // Observe pinsList items and show or hide empty pin text.
        observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (adapter.getItemCount() > 0) {
                    loadingText.setVisibility(View.GONE);
                    progressbar.setVisibility(View.GONE);
                }

            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                if (adapter.getItemCount() <= 0) {
                    if (loadingText.getVisibility() == View.GONE) {
                        loadingText.setVisibility(View.VISIBLE);
                        loadingText.setText(R.string.home_pins_empty);
                    }
                }
            }
        };

        adapter = new PinAdapter(PinItem.class, R.layout.single_pin_item, PinHolder.class, pinRef, this);
        pinsList.setAdapter(adapter);
        adapter.registerAdapterDataObserver(observer);

        /**
         * Hide the progressbar here.
         * Firebase calls Value event after Child events.
         * Child events are used by FirebaseUI RecyclerView internally.
         */
        pinRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() <= 0) {
                    progressbar.setVisibility(View.GONE);
                    loadingText.setText(R.string.home_pins_empty);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Do nothing
            }
        });

    }

    /**
     * Recommended to cleanup the adapter for Firebase UI RecyclerView.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adapter != null)
            adapter.cleanup();
    }

    /**
     * Called when unpin icon is clicked.
     *
     * @param key Key for the item to unpin.
     */
    @Override
    public void onUnpin(final String key) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(getActivity());
        confirmDialog.setTitle("Unpin?")
                .setMessage("This will be removed from all of your devices")
                .setPositiveButton("Unpin", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FireClipUtils.unpinItem(key).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "Couldn't unpin!", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "onFailure: Reason: " + e.getMessage());
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        confirmDialog.create().show();
    }

    /**
     * Optional method.
     */
    @Override
    public void onItemClick(PinItem pinItem) {
        // Nothing to do!
    }
}
