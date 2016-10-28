package com.abara.fireclip.util;

import android.view.View;

/**
 * Created by abara on 09/10/16.
 */

/*
* Callback listeners for Favourite items.
* */
public interface FavItemClickListener {

    public void onRemoveClick(String key);

    public void onItemClick(Favourite favourite);

}
