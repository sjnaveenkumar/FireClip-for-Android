package com.abara.fireclip.util;

import io.realm.RealmObject;
import io.realm.annotations.Index;

/**
 * Created by abara on 08/09/16.
 */

/*
* A plain HistoryClip JAVA class
* */
public class HistoryClip extends RealmObject {

    private String content;
    private long timestamp;
    @Index
    private String from;

    public HistoryClip() {
    }

    public HistoryClip(String content, long timestamp, String from) {
        this.content = content;
        this.timestamp = timestamp;
        this.from = from;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
