package com.abara.fireclip.util;

/**
 *
 * A plain PinItem JAVA class
 *
 * Created by abara on 08/10/16.
 */
public class PinItem {

    private String content, from, key_fav;
    private long timestamp_fav;
    private long timestamp;

    public PinItem() {
    }

    public PinItem(String content, long timestamp, String from, String key_fav, long timestamp_fav) {
        this.content = content;
        this.timestamp = timestamp;
        this.from = from;
        this.key_fav = key_fav;
        this.timestamp_fav = timestamp_fav;
    }

    public String getKey_fav() {
        return key_fav;
    }

    public void setKey_fav(String key_fav) {
        this.key_fav = key_fav;
    }

    public long getTimestamp_fav() {
        return timestamp_fav;
    }

    public void setTimestamp_fav(long timestamp_fav) {
        this.timestamp_fav = timestamp_fav;
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
