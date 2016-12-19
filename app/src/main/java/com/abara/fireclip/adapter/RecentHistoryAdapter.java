package com.abara.fireclip.adapter;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.abara.fireclip.R;
import com.abara.fireclip.util.HistoryClip;
import com.abara.fireclip.util.ItemClickListener;

import io.realm.RealmResults;

/**
 * <p>RecyclerView adapter to populate 5 items from History,
 * including a Header and a Footer.</p>
 * <p>
 * Created by abara on 08/09/16.
 */
public class RecentHistoryAdapter extends HistoryAdapter {

    private static final int HEADER_ITEM = 0;
    private static final int CLIP_ITEM = 1;
    private static final int FOOTER_ITEM = 2;

    private static final int HEADER_FOOTER_COUNT = 2;
    private static final int MAX_ITEM_COUNT = 5;

    public RecentHistoryAdapter(Context context, RealmResults<HistoryClip> historyClips, ItemClickListener itemClickListener) {
        super(context, historyClips, itemClickListener);
    }

    /**
     * Return the type of view holder for the view types.
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == HEADER_ITEM) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_header, parent, false);
            return new HeaderHolder(view);
        } else if (viewType == FOOTER_ITEM) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_footer, parent, false);
            return new FooterHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
            return new HistoryHolder(view);
        }

    }

    /**
     * Populate view corresponding to it's position.
     * Position 0 : Header.
     * Position last-1 : Footer.
     * in-between : History items.
     */
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

        // Populate history items between first and last.
        if (position != 0 && position != getItemCount() - 1) {
            populateHistoryItems((HistoryHolder) holder, position - 1);
            return;
        }

        // Populate footer as last item.
        if (position == getItemCount() - 1) {
            final FooterHolder footerHolder = (FooterHolder) holder;
            footerHolder.viewMoreLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    itemClickListener.onItemClick(-1);
                }
            });
        }

    }

    /**
     * Return the view type.
     * First item is header.
     * Last item is footer.
     * Other items are history items.
     */
    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return HEADER_ITEM;
        if (position == getItemCount() - 1)
            return FOOTER_ITEM;
        return CLIP_ITEM;
    }

    /**
     * Calculate and return the item count.
     * If no. of history items > 5, return 5 + HEADER_FOOTER_COUNT (2)
     * else return size of history items + HEADER_FOOTER_COUNT (2)
     */
    @Override
    public int getItemCount() {
        int size = historyClips.size();
        if (size < MAX_ITEM_COUNT) {
            return size + HEADER_FOOTER_COUNT;
        }
        return MAX_ITEM_COUNT + HEADER_FOOTER_COUNT;
    }
}

/**
 * View holder for Footer item.
 */
class FooterHolder extends RecyclerView.ViewHolder {

    LinearLayout viewMoreLayout;

    FooterHolder(View itemView) {
        super(itemView);
        viewMoreLayout = (LinearLayout) itemView.findViewById(R.id.footer_view_more);
    }
}

/**
 * View holder for Header item.
 */
class HeaderHolder extends RecyclerView.ViewHolder {
    HeaderHolder(View itemView) {
        super(itemView);
    }
}

/**
 * View holder for History item.
 */
class HistoryHolder extends RecyclerView.ViewHolder {

    AppCompatTextView from, content;
    LinearLayout itemLayout, pinItLayout;
    AppCompatImageView copyButton;
    AppCompatTextView pinItText;
    CardView card;

    HistoryHolder(View view) {
        super(view);

        from = (AppCompatTextView) view.findViewById(R.id.history_item_from);
        content = (AppCompatTextView) view.findViewById(R.id.history_item_content);
        itemLayout = (LinearLayout) view.findViewById(R.id.history_item_layout);
        copyButton = (AppCompatImageView) view.findViewById(R.id.history_item_copy);
        pinItLayout = (LinearLayout) view.findViewById(R.id.history_item_layout_pin_it);

        pinItText = (AppCompatTextView) view.findViewById(R.id.history_pin_it);

        card = (CardView) view.findViewById(R.id.history_item_card);
    }

}
