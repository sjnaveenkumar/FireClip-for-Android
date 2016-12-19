package com.abara.fireclip.util;

/**
 * Callback listeners for PinItems.
 * <p>
 * Created by abara on 09/10/16.
 */
public interface PinItemClickListener {

    void onUnpin(String key);

    void onItemClick(PinItem pinItem);

}
