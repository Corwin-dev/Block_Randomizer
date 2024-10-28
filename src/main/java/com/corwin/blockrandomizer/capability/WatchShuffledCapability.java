package com.corwin.blockrandomizer.capability;

import net.minecraft.nbt.CompoundTag;

public class WatchShuffledCapability implements IWatchShuffledCapability {
    private boolean shuffled;
    private boolean initialized;

    @Override
    public void setShuffled(boolean value) {
        this.shuffled = value;
    }

    @Override
    public boolean isShuffled() {
        return this.shuffled;
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public void initialize() {
        this.shuffled = false; // Default value
        this.initialized = true;
    }

    // Save to NBT
    public void saveNBTData(CompoundTag nbt) {
        nbt.putBoolean("shuffled", this.shuffled);
        nbt.putBoolean("Initialized", this.initialized);
    }

    // Load from NBT
    public void loadNBTData(CompoundTag nbt) {
        this.shuffled = nbt.getBoolean("Shuffled");
        this.initialized = nbt.getBoolean("Initialized");
    }
}
