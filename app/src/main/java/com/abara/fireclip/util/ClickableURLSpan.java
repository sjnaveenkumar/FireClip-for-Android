package com.abara.fireclip.util;

import android.text.style.ClickableSpan;
import android.view.View;

/**
 * Created by abara on 13/10/16.
 */

public class ClickableURLSpan extends ClickableSpan {

    private View.OnClickListener listener;

    public ClickableURLSpan(View.OnClickListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View widget) {
        this.listener.onClick(widget);
    }
}
