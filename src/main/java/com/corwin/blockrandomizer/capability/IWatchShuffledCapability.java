package com.corwin.blockrandomizer.capability;

public interface IWatchShuffledCapability {
    void setShuffled(boolean value);
    boolean isShuffled();
    boolean isInitialized();
    void initialize();
}
