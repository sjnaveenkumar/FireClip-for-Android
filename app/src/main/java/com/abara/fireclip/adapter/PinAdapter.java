package com.abara.fireclip.adapter;

import android.view.View;

import com.abara.fireclip.util.PinItem;
import com.abara.fireclip.util.PinItemClickListener;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;

/**
 *
 * <p>
 * Adapter to populate PinItems. Subclass of <b>FirebaseRecyclerAdapter</b> which handles all child events at a
 * given Firebase location.
 * </p>
 *
 * <p>Here the location is <b>pinRef</b></p>
 *
 * The structure of pinRef location is:
 *          $userUID
 *              |
 *              |-pins -> pinRef
 *                  |
 *                  |-$pinUID -> PinItem1
 *                  |-$pinUID -> PinItem2
 *                  |-$pinUID -> PinItem3
 *
 * Created by abara on 09/10/16.
 */
public class PinAdapter extends FirebaseRecyclerAdapter<PinItem, PinHolder> {

    private PinItemClickListener listener;

    public PinAdapter(Class<PinItem> pinItemClass, int pinLayout, Class<PinHolder> pinHolder, DatabaseReference pinRef, PinItemClickListener listener) {
        super(pinItemClass, pinLayout, pinHolder, pinRef);
        this.listener = listener;
    }

    @Override
    protected void populateViewHolder(final PinHolder holder, final PinItem pinItem, int position) {

        holder.content.setText(pinItem.getContent());
        holder.closeAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onUnpin(pinItem.getKey_fav());
            }
        });
        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClick(pinItem);
            }
        });

    }
}