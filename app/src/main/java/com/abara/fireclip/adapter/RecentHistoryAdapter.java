package com.abara.fireclip.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.util.Linkify;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.abara.fireclip.R;
import com.abara.fireclip.util.HistoryClip;
import com.abara.fireclip.util.ItemClickListener;
import com.abara.fireclip.util.Utils;

import io.realm.RealmResults;

/**
 * Created by abara on 08/09/16.
 */

/*
* RecyclerView adapter to populate 5 items from History,
* including a Header and a Footer.
* */

public class RecentHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int HEADER_ITEM = 0;
    private static final int CLIP_ITEM = 1;
    private static final int FOOTER_ITEM = 2;

    private static final int HEADER_FOOTER_COUNT = 2;
    private static final int MAX_ITEM_COUNT = 5;

    private RealmResults<HistoryClip> historyClips;
    private Context context;
    private ItemClickListener itemClickListener;

    private LinearLayout lastTagLayout;

    public RecentHistoryAdapter(Context context, RealmResults<HistoryClip> historyClips, ItemClickListener itemClickListener) {
        this.historyClips = historyClips;
        this.context = context;
        this.itemClickListener = itemClickListener;
    }

    /*
    * Return the type of view holder for the view type.
    * */
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

    /*
    * Populate view corresponding to it's position.
    * Position 0 : Header is always first.
    * Position last-1 : Footer is always last.
    * else : in between items.
    * */
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

        // Populate history items between first and last.
        if (position != 0 && position != getItemCount() - 1) {
            final HistoryHolder historyHolder = (HistoryHolder) holder;
            final HistoryClip clip = historyClips.get(position - 1);
            historyHolder.from.setText(clip.getFrom() + " • " + Utils.getTimeSince(clip.getTimestamp()));
            historyHolder.content.setText(clip.getContent());
            historyHolder.copyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipData data = ClipData.newPlainText("FireClipText", clip.getContent());
                    ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    manager.setPrimaryClip(data);
                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
                }
            });
            historyHolder.itemLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    itemClickListener.onLongClick(holder.getAdapterPosition() - 1);
                    return true;
                }
            });
            historyHolder.itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (historyHolder.addToFavLayout.getVisibility() == View.GONE) {
                        if (lastTagLayout != null)
                            lastTagLayout.setVisibility(View.GONE);
                        lastTagLayout = historyHolder.addToFavLayout;
                        // Expand card and show addToFavLayout.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            TransitionManager.beginDelayedTransition(historyHolder.card);
                        }
                        historyHolder.addToFavLayout.setVisibility(View.VISIBLE);
                    } else {
                        // TODO: A way to implement same animation while hiding addToFavLayout.
                        historyHolder.addToFavLayout.setVisibility(View.GONE);
                    }
                }
            });
            historyHolder.favTag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemClickListener.onFavouriteClick(clip.getContent(), clip.getFrom(), clip.getTimestamp());
                }
            });
            Linkify.addLinks(historyHolder.content, Linkify.ALL);
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

    /*
    * Return the view type.
    * If position - 0 : return HEADER_ITEM
    * else if position - last :  return FOOTER_ITEM
    * else CLIP_ITEM
    * */
    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return HEADER_ITEM;
        if (position == getItemCount() - 1)
            return FOOTER_ITEM;
        return CLIP_ITEM;
    }

    /*
    * Calculate and return the item count.
    * If no. of history items is 5, return 5 + HEADER_FOOTER_COUNT (2)
    * else return size of history items + HEADER_FOOTER_COUNT (2)
    * */
    @Override
    public int getItemCount() {
        int size = historyClips.size();
        if (size < MAX_ITEM_COUNT) {
            return size + HEADER_FOOTER_COUNT;
        }
        return MAX_ITEM_COUNT + HEADER_FOOTER_COUNT;
    }
}

/*
* View holder for History Item.
* */
class HistoryHolder extends RecyclerView.ViewHolder {

    AppCompatTextView from, content;
    LinearLayout itemLayout, addToFavLayout;
    AppCompatImageView copyButton;
    AppCompatTextView favTag;
    CardView card;

    public HistoryHolder(View view) {
        super(view);

        from = (AppCompatTextView) view.findViewById(R.id.history_item_from);
        content = (AppCompatTextView) view.findViewById(R.id.history_item_content);
        itemLayout = (LinearLayout) view.findViewById(R.id.history_item_layout);
        copyButton = (AppCompatImageView) view.findViewById(R.id.history_item_copy);
        addToFavLayout = (LinearLayout) view.findViewById(R.id.history_item_layout_add_fav);

        favTag = (AppCompatTextView) view.findViewById(R.id.history_add_to_fav);

        card = (CardView) view.findViewById(R.id.history_item_card);
    }
}

/*
* View holder for Header item.
* */
class HeaderHolder extends RecyclerView.ViewHolder {
    public HeaderHolder(View itemView) {
        super(itemView);
    }
}

/*
* View holder for Footer item.
* */
class FooterHolder extends RecyclerView.ViewHolder {

    LinearLayout viewMoreLayout;
    public FooterHolder(View itemView) {
        super(itemView);
        viewMoreLayout = (LinearLayout) itemView.findViewById(R.id.footer_view_more);
    }
}