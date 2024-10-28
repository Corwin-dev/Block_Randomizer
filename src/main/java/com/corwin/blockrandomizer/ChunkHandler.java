package com.corwin.blockrandomizer;

import com.corwin.blockrandomizer.capability.CapabilityHandler;
import net.minecraft.core.BlockPos;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod.EventBusSubscriber(modid = BlockRandomizer.MODID)
public class ChunkHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Standard block shuffling event when the chunk loads
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        
        String exeGroupName = "onLoad";
        if (BlockPoolHandler.getConsolidatedExeTable(exeGroupName).isEmpty()) return;
        
        ChunkAccess chunkAccess = event.getChunk();
        LevelChunk levelChunk = (LevelChunk) chunkAccess;

        // Check if the chunk has already been shuffled
        levelChunk.getCapability(CapabilityHandler.LOAD_SHUFFLED_CAPABILITY).ifPresent(cap -> {
        if (cap.isShuffled()) return;
            cap.setShuffled(true);
        });
        
        LOGGER.info("Chunk Shuffler: Processing group '{}' for chunk load event", exeGroupName);
        shuffleChunkBlocks(serverLevel, chunkAccess, exeGroupName);
    }

    // Delayed shuffling for blocks that cause cascading updates or something, idk, it just works.
    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        String exeGroupName = "onWatch";
        if (BlockPoolHandler.getConsolidatedExeTable(exeGroupName).isEmpty()) return;
        
        ServerLevel serverLevel = (ServerLevel) event.getLevel();
        ChunkAccess chunkAccess = event.getChunk();
        LevelChunk levelChunk = (LevelChunk) chunkAccess;
        Boolean doChunkReload = false;
        
        // Check if the chunk has already delayed shuffled
        levelChunk.getCapability(CapabilityHandler.DELAYED_SHUFFLED_CAPABILITY).ifPresent(cap -> {
        if (cap.isDelayedShuffled()) return;
            cap.setDelayedShuffled(true);
        });

        
        LOGGER.info("Delayed Chunk Shuffler: Processing group '{}' for chunk watch event", exeGroupName);
        // Force chunk reload if any blocks were changed, for player visibility update
        if (shuffleChunkBlocks(serverLevel, chunkAccess, exeGroupName)) {
            forceChunkReload(event.getPlayer(), serverLevel, levelChunk);   
        }
    }

    // Method to shuffle chunk blocks based on group
    private static boolean shuffleChunkBlocks(ServerLevel level, ChunkAccess chunk, String exeGroupName) {
        Boolean result = false;
        for (int x = 0; x < 16; x++) {
            for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    updateBlockStateIfDifferent(level, chunk, x, y, z, exeGroupName);
                }
            }
        }
        return false;
    }
    // I dont know what this does, ChatGPT needed it
    private static <T extends Comparable<T>> BlockState setPropertyValue(BlockState state, Property<T> property, Comparable<?> value) {
        return state.setValue(property, property.getValueClass().cast(value));
    }

    // Replaces the block, if needed, according to the given chunk and chunk-local block position
    private static void updateBlockStateIfDifferent(ServerLevel level, ChunkAccess chunk, int x, int y, int z, String exeGroupName) {
        BlockPos blockPos = chunk.getPos().getBlockAt(x, y, z);
        BlockState originalBlockState = chunk.getBlockState(blockPos);
        Block newBlock = TableHandler.getConsolidatedExeTable(exeGroupName).getOrDefault(originalBlockState.getBlock(), originalBlockState.getBlock());

        // Make sure the block needs to change, adjust properties, and replace
        if (!originalBlockState.is(newBlock)) {
            BlockState newBlockState = newBlock.defaultBlockState();

            // Transfer properties with type casting
            for (var property : originalBlockState.getProperties()) {
                if (newBlockState.getProperties().contains(property)) {
                    newBlockState = setPropertyValue(newBlockState, property, originalBlockState.getValue(property));
                }
            }

            // Turn off leaf decay, to reduce lag
            if (newBlock instanceof LeavesBlock) {
                newBlockState = newBlockState.setValue(LeavesBlock.PERSISTENT, true);
            }
            // Replace block
            chunk.setBlockState(blockPos, newBlockState, false);    
        }
    }
    
    // Forces the client to reload tbe chunk, since blocks were changed
    public static void forceChunkReload(ServerPlayer player, ServerLevel level, LevelChunk chunk) {
        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null, true);
        player.connection.send(packet);
    }

    // Uses a server level in-game command to set the block, to ensure its properly configured the way minecraft needs it to be
    public static void setBlockWithCommand(ServerLevel level, ChunkAccess chunk, BlockPos blockPos, BlockState blockState) {
        // Use ForgeRegistries to get the registry name of the block
        String blockString = ForgeRegistries.BLOCKS.getKey(blockState.getBlock()).toString(); 
        
        // Construct the command string, utilizing blockPos and blockState
        String command = String.format("setblock %d %d %d %s", blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockString);

        // Create a CommandSourceStack for executing the command
        CommandSourceStack source = level.getServer().createCommandSourceStack()
                .withPosition(Vec3.atCenterOf(blockPos))
                .withPermission(2) // Admin-level permissions
                .withLevel(level)
                .withSuppressedOutput();

        // Parse and execute the command
        try {
             level.getServer().getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }
}
