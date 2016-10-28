package com.abara.fireclip.adapter;

import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import com.abara.fireclip.R;

/**
 * Created by abara on 09/10/16.
 */

/*
* View holder for Favourites items.
* */

public class FavHolder extends RecyclerView.ViewHolder {

    public AppCompatTextView content;
    public AppCompatImageView closeAction;
    public LinearLayout card;

    public FavHolder(View itemView) {
        super(itemView);

        content = (AppCompatTextView) itemView.findViewById(R.id.favourite_item_text);
        closeAction = (AppCompatImageView) itemView.findViewById(R.id.favourite_item_close);
        card = (LinearLayout) itemView.findViewById(R.id.favourite_item_layout);

    }
}
