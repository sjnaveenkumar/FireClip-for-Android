package com.abara.fireclip.fragment;


import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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

import com.abara.fireclip.R;
import com.abara.fireclip.adapter.FavHolder;
import com.abara.fireclip.adapter.FavouritesAdapter;
import com.abara.fireclip.util.FavItemClickListener;
import com.abara.fireclip.util.Favourite;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by abara on 09/10/16.
 */

public class FavouritesFragment extends Fragment implements FavItemClickListener {

    private static final String TAG = FavouritesFragment.class.getSimpleName();
    private DatabaseReference favRef;
    private FirebaseUser clipUser;

    private RecyclerView favoritesList;
    private AppCompatTextView loadingText;
    private ProgressBar progressbar;

    private FavouritesAdapter adapter;
    private RecyclerView.AdapterDataObserver observer;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        clipUser = FirebaseAuth.getInstance().getCurrentUser();
        return inflater.inflate(R.layout.fragment_favourites, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        loadingText = (AppCompatTextView) view.findViewById(R.id.home_fav_empty_text);
        progressbar = (ProgressBar) view.findViewById(R.id.home_fav_progress);
        favoritesList = (RecyclerView) view.findViewById(R.id.home_favorites_list);
        favoritesList.setHasFixedSize(false);
        favoritesList.setItemAnimator(new DefaultItemAnimator());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        favoritesList.setLayoutManager(linearLayoutManager);

        favRef = FirebaseDatabase.getInstance().getReference("users").child(clipUser.getUid()).child("fav");

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
                        loadingText.setText(R.string.home_favorites_empty);
                    }
                }
            }
        };

        adapter = new FavouritesAdapter(Favourite.class, R.layout.single_item_favourite, FavHolder.class, favRef, this);
        favoritesList.setAdapter(adapter);
        adapter.registerAdapterDataObserver(observer);

        favRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() <= 0) {
                    progressbar.setVisibility(View.GONE);
                    loadingText.setText(R.string.home_favorites_empty);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Do nothing
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adapter != null)
            adapter.cleanup();
    }

    @Override
    public void onRemoveClick(final String key) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(getActivity());
        confirmDialog.setTitle("Remove?")
                .setMessage("This will be removed from all of your devices")
                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        favRef.child(key).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Snackbar.make(getView(), "Favourite removed!", Snackbar.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(getView(), "Can't remove favourite!", Snackbar.LENGTH_SHORT).show();
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

    @Override
    public void onItemClick(Favourite favourite) {


    }
}
