package com.example.crudoperation.abm;

import java.util.Collections;
import java.util.List;

/**
 * One page from Apple's fetch or sync device list APIs.
 */
public class DepDevicePage {
    private final String cursor;
    private final boolean moreToFollow;
    private final List<String> serialNumbers;

    public DepDevicePage(String cursor, boolean moreToFollow, List<String> serialNumbers) {
        this.cursor = cursor;
        this.moreToFollow = moreToFollow;
        this.serialNumbers = serialNumbers == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(serialNumbers);
    }

    public String getCursor() {
        return cursor;
    }

    public boolean isMoreToFollow() {
        return moreToFollow;
    }

    public List<String> getSerialNumbers() {
        return serialNumbers;
    }
}
