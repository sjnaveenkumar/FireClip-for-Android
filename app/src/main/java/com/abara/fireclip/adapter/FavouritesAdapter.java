package com.abara.fireclip.adapter;

import android.view.View;

import com.abara.fireclip.util.FavItemClickListener;
import com.abara.fireclip.util.Favourite;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

/**
 * Created by abara on 09/10/16.
 */

/*
* RecyclerView adapter to populate Favourites items.
* */

public class FavouritesAdapter extends FirebaseRecyclerAdapter<Favourite, FavHolder> {

    private FavItemClickListener listener;

    public FavouritesAdapter(Class<Favourite> modelClass, int modelLayout, Class<FavHolder> viewHolderClass, DatabaseReference ref, FavItemClickListener listener) {
        super(modelClass, modelLayout, viewHolderClass, ref);
        this.listener = listener;
    }

    @Override
    protected void populateViewHolder(final FavHolder holder, final Favourite favourite, int position) {

        holder.content.setText(favourite.getContent());
        holder.closeAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRemoveClick(favourite.getKey_fav());
            }
        });
        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(favourite);
            }
        });

    }
}