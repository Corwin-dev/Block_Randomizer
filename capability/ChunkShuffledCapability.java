package com.corwin.blockshuffler.capability;

public class ChunkShuffledCapability implements IChunkShuffledCapability {
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
