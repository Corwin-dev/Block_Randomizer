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

public class ChunkShuffledProvider extends CapabilityProvider<ChunkShuffledProvider> implements ICapabilitySerializable<CompoundTag> {
    private final IChunkShuffledCapability instance = new ChunkShuffledCapability();
    private final LazyOptional<IChunkShuffledCapability> lazyOptional = LazyOptional.of(() -> instance);

    public ChunkShuffledProvider() {
        super(ChunkShuffledProvider.class);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return CapabilityHandler.CHUNK_SHUFFLED_CAPABILITY.orEmpty(cap, lazyOptional);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("isShuffled", instance.isShuffled());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        instance.setShuffled(nbt.getBoolean("isShuffled"));
    }
}
