package com.abara.fireclip.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
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
 * Created by abara on 11/09/16.
 */

public class HistoryAdapter extends RecyclerView.Adapter<HistoryHolder> {

    private Context context;
    private RealmResults<HistoryClip> historyClips;
    private ItemClickListener itemClickListener;

    private LinearLayout lastTagLayout;

    public HistoryAdapter(Context context, RealmResults<HistoryClip> historyClips, ItemClickListener itemClickListener) {
        this.context = context;
        this.historyClips = historyClips;
        this.itemClickListener = itemClickListener;
    }

    @Override
    public HistoryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryHolder(view);
    }

    @Override
    public void onBindViewHolder(final HistoryHolder holder, int position) {

        final HistoryClip clip = historyClips.get(position);
        holder.from.setText(clip.getFrom() + " • " + Utils.getTimeSince(clip.getTimestamp()));
        holder.content.setText(clip.getContent());
        holder.copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipData data = ClipData.newPlainText("FireClipText", clip.getContent());
                ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                manager.setPrimaryClip(data);
                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });
        holder.itemLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                itemClickListener.onLongClick(holder.getAdapterPosition());
                return true;
            }
        });
        holder.itemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.tagsLayout.getVisibility() == View.GONE) {
                    if (lastTagLayout != null)
                        lastTagLayout.setVisibility(View.GONE);
                    lastTagLayout = holder.tagsLayout;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        TransitionManager.beginDelayedTransition(holder.card);
                    }
                    holder.tagsLayout.setVisibility(View.VISIBLE);
                } else {
                    holder.tagsLayout.setVisibility(View.GONE);
                }
            }
        });
        holder.favTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onFavouriteClick(clip.getContent(), clip.getFrom(), clip.getTimestamp());
            }
        });
        Linkify.addLinks(holder.content, Linkify.ALL);

    }

    @Override
    public int getItemCount() {
        return historyClips.size();
    }
}
