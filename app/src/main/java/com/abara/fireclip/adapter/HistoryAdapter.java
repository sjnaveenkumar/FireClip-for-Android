package com.abara.fireclip.adapter;

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
import com.abara.fireclip.util.AndroidUtils;
import com.abara.fireclip.util.HistoryClip;
import com.abara.fireclip.util.ItemClickListener;

import io.realm.RealmResults;

/**
 * <p>Adapter to populate all history items.</p>
 * <p>
 * Created by abara on 11/09/16.
 */
public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected Context context;
    protected RealmResults<HistoryClip> historyClips;
    protected ItemClickListener itemClickListener;

    private LinearLayout lastHistoryLayout;

    public HistoryAdapter(Context context, RealmResults<HistoryClip> historyClips, ItemClickListener itemClickListener) {
        this.context = context;
        this.historyClips = historyClips;
        this.itemClickListener = itemClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        populateHistoryItems((HistoryHolder) holder, position);

    }

    public void expandItem(HistoryHolder holder) {
        if (holder.pinItLayout.getVisibility() == View.GONE) {
            if (lastHistoryLayout != null)
                lastHistoryLayout.setVisibility(View.GONE);
            lastHistoryLayout = holder.pinItLayout;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                TransitionManager.beginDelayedTransition(holder.card);
            }
            holder.pinItLayout.setVisibility(View.VISIBLE);
        } else {
            holder.pinItLayout.setVisibility(View.GONE);
        }
    }

    protected void populateHistoryItems(final HistoryHolder holder, int position) {
        final HistoryClip clip = historyClips.get(position);
        holder.from.setText(clip.getFrom() + " • " + AndroidUtils.getTimeSince(clip.getTimestamp()));
        holder.content.setText(clip.getContent());
        holder.copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AndroidUtils.copyToClipboard(context, clip.getContent());
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
                expandItem(holder);
            }
        });
        holder.pinItText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onAddPin(clip);
            }
        });
        Linkify.addLinks(holder.content, Linkify.ALL);
    }

    @Override
    public int getItemCount() {
        return historyClips.size();
    }
}
