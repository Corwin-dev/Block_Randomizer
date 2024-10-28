package com.corwin.blockshuffler.capability;

public interface IDelayedShuffledCapability {
    void setDelayedShuffled(boolean value);
    boolean isDelayedShuffled();
    boolean isInitialized();
    void initialize();
}
