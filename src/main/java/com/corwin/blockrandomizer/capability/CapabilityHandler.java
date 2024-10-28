package com.corwin.blockrandomizer.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import com.corwin.blockshuffler.BlockShuffler;

@Mod.EventBusSubscriber(modid = BlockShuffler.MODID)
public class CapabilityHandler {
    public static final Capability<IChunkShuffledCapability> CHUNK_SHUFFLED_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});
    public static final Capability<IDelayedShuffledCapability> DELAYED_SHUFFLED_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IChunkShuffledCapability.class);
        event.register(IDelayedShuffledCapability.class); // Register the new capability
    }
    
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        if (!event.getObject().getLevel().isClientSide()) {
            event.addCapability(new ResourceLocation(BlockShuffler.MODID, "chunk_shuffled"), new ChunkShuffledProvider());
         }
    }
}
