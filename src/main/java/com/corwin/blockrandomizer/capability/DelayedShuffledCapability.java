package com.corwin.blockshuffler.capability;

import net.minecraft.nbt.CompoundTag;

public class DelayedShuffledCapability implements IDelayedShuffledCapability {
    private boolean delayedShuffled;
    private boolean initialized;

    @Override
    public void setDelayedShuffled(boolean value) {
        this.delayedShuffled = value;
    }

    @Override
    public boolean isDelayedShuffled() {
        return this.delayedShuffled;
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    public void initialize() {
        this.delayedShuffled = false; // Default value
        this.initialized = true;
    }

    // Save to NBT
    public void saveNBTData(CompoundTag nbt) {
        nbt.putBoolean("DelayedShuffled", this.delayedShuffled);
        nbt.putBoolean("Initialized", this.initialized);
    }

    // Load from NBT
    public void loadNBTData(CompoundTag nbt) {
        this.delayedShuffled = nbt.getBoolean("DelayedShuffled");
        this.initialized = nbt.getBoolean("Initialized");
    }
}
