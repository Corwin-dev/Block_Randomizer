package com.corwin.blockrandomizer;

import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(BlockBandomizer.MODID)
public class BlockRandomizer {
    public static final String MODID = "blockrandomizer";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Mod Initialization
    public BlockRandomizer() {
        LOGGER.info("Block Randomizer: Initialization started");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Block Randomizer: Initialization complete");
    }

    // World is generating, setup and shuffle the block tables
    @SubscribeEvent
    public void onWorldGen(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LOGGER.info("Block Randomizer: World generation detected on server side.");
            // Initialize tables dynamically
            TableHandler.initializeBlockGroups(level);
        }
    }

    // World is loading, load the block tables from JSON, if present.
    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            LOGGER.info("Block Randomizer: World loading on server side.");
            // Load each group’s block table from JSON
            TableHandler.loadBlockGroups(level);
        }
    }
}
