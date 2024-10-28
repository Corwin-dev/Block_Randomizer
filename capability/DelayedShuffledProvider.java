package com.corwin.blockshuffler.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.ObjectHolder;
import com.corwin.blockshuffler.BlockShuffler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DelayedShuffledProvider extends CapabilityProvider<DelayedShuffledProvider> implements ICapabilitySerializable<CompoundTag> {
    private final IDelayedShuffledCapability instance = new DelayedShuffledCapability();
    private final LazyOptional<IDelayedShuffledCapability> lazyOptional = LazyOptional.of(() -> instance);

    public DelayedShuffledProvider() {
        super(DelayedShuffledProvider.class);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return CapabilityHandler.DELAYED_SHUFFLED_CAPABILITY.orEmpty(cap, lazyOptional);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isDelayedShuffled", instance.isDelayedShuffled());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.setDelayedShuffled(nbt.getBoolean("isDelayedShuffled"));
    }
}
