package com.corwin.blockrandomizer.capability;

public class LoadShuffledCapability implements ILoadShuffledCapability {
    private boolean shuffled = false;
    
    @Override
    public boolean isShuffled() {
        return shuffled;
    }

    @Override
    public void setShuffled(boolean shuffled) {
        this.shuffled = shuffled;
    }
}
