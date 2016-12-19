package com.abara.fireclip.util;

/**
 *
 * Callback listeners for History item.
 *
 * Created by abara on 11/09/16.
 */
public interface ItemClickListener {

    void onItemClick(int position);

    void onLongClick(int position);

    void onAddPin(HistoryClip historyClip);

}
