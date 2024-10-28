package com.corwin.blockshuffler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.BiPredicate;

public class TableHandler {
    private static final Map<String, Map<Block, Block>> blockGroups = new HashMap<>();
    private static final Random random = new Random();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Map<String, TriPredicate<Block, BlockState, Level>>> exeGroups = new HashMap<>();
    private static final Map<String, TriPredicate<Block, BlockState, Level>> filters = new LinkedHashMap<>();
    private static Map<String, Map<Block, Block>> consolidatedExeTables = new HashMap<>();
    
    // Appends the new blockgroup and filter to the execution group
    private static void addToExeGroup(String exeStage, String blockGroup, TriPredicate<Block, BlockState, Level> filterCriteria) {
        Map<String, TriPredicate<Block, BlockState, Level>> exeMap = exeGroups.getOrDefault(exeStage, new HashMap<>());
        exeMap.put(blockGroup, filterCriteria);
        filters.put(blockGroup, filterCriteria);
        exeGroups.put(exeStage, exeMap);
    }

    // Runs at startup to hardcode the execution groups
    static {
        addToExeGroup("exclude", "RedstoneComponentBlocks", TableHandler::isRedstoneComponentBlock);
        addToExeGroup("exclude", "AirBlocks", TableHandler::isAirBlock);
        addToExeGroup("exclude", "FireBlocks", TableHandler::isFireBlock);
        addToExeGroup("exclude", "IndestructibleBlocks", TableHandler::isIndestructibleBlock);
        addToExeGroup("exclude", "ProblemBlocks", TableHandler::isProblemBlock);
        addToExeGroup("exclude", "EntityBlocks", TableHandler::isEntityBlock);
        addToExeGroup("exclude", "MenuBlocks", TableHandler::isMenuBlock);
        addToExeGroup("onLoad", "AquaticBlocks", TableHandler::isAquaticBlock);
        addToExeGroup("onLoad", "DelicateObjectBlocks", TableHandler::isDelicateObjectBlock);
        addToExeGroup("onLoad", "SlabBlocks", TableHandler::isSlabBlock);
        addToExeGroup("onLoad", "StairBlocks", TableHandler::isStairBlock);
        addToExeGroup("onLoad", "PlainBlocks", TableHandler::isPlainBlock);
    }

    // Checks if the given block group is in the queried execution group
    public static boolean isInExeGroup(String exeGroup, String blockGroup) {
        return (exeGroups.containsKey(exeGroup) && exeGroups.get(exeGroup).containsKey(blockGroup));
    }

    // Begin the table building process of filtering and shuffling blocks into grouped maps
    public static void initializeBlockGroups(ServerLevel level) {
        for (Map.Entry<String, TriPredicate<Block, BlockState, Level>> entry : filters.entrySet()) {
            String blockGroup = entry.getKey();    // Gets the key (e.g., "apple")
            TriPredicate<Block, BlockState, Level> filterCriteria = entry.getValue(); // Gets the value (e.g., 3)

            LOGGER.info("Block Shuffler: Initializing block group '{}'", blockGroup);
                   
            initializeTable(level, blockGroup, filterCriteria);
        }
        consolidatedExeTables = consolidateExecutionGroups();
    }

    public static Map<String, Map<Block, Block>> consolidateExecutionGroups() {
        Map<String, Map<Block, Block>> consolidatedTables = new HashMap<>();

        // Iterate over each execution group (e.g., "onLoad", "onWatch")
        for (String exeGroup : exeGroups.keySet()) {
            Map<Block, Block> mergedTable = new HashMap<>();

            // For each block group in the current execution group
            for (String blockGroup : exeGroups.get(exeGroup).keySet()) {
                Map<Block, Block> blockTable = getTable(blockGroup);

                // Merge the block table entries into the merged table for the exeGroup
                for (Map.Entry<Block, Block> entry : blockTable.entrySet()) {
                    Block sourceBlock = entry.getKey();
                    Block targetBlock = entry.getValue();

                   // Avoid overriding existing mappings to prioritize first-encountered mappings
                    mergedTable.putIfAbsent(sourceBlock, targetBlock);
                }
            }
        
            // Store the merged table in the consolidatedTables under the execution group name
            consolidatedTables.put(exeGroup, mergedTable);
        }

        // Optional: Log the consolidated tables for verification
        LOGGER.info("Consolidated Execution Groups: {}", consolidatedTables.keySet());
        for (String exeGroup : consolidatedTables.keySet()) {
            LOGGER.info("Execution Group '{}' contains {} mappings.", exeGroup, consolidatedTables.get(exeGroup).size());
        }

        return consolidatedTables;
    }

    // Load up the worlds tables from save foles, if present
    public static void loadBlockGroups(Level level) {
        if (!blockGroups.isEmpty()) return;
        for (String blockGroup : blockGroups.keySet()) {
            LOGGER.info("Block Shuffler: Loading block group '{}' from JSON", blockGroup);
            loadTableFromJson(level, blockGroup);
        }
    }
    
    // Method to get a table by its name
    public static Map<Block, Block> getTable(String blockGroup) {
        return blockGroups.getOrDefault(blockGroup, Collections.emptyMap());
    }
    
    public static Map<Block, Block> getConsolidatedExeTable(String exeGroupName) {
        return consolidatedExeTables.getOrDefault(exeGroupName, Collections.emptyMap());
    }
    
    // Method to get all tables
    public static Map<String, Map<Block, Block>> getAllTables() {
        return blockGroups;
    }

    // Create, filter, shuffle, and save a table of block pairs
    private static void initializeTable(Level level, String blockGroup, TriPredicate<Block, BlockState, Level> filterCriteria) {
        LOGGER.info("Block Shuffler: Initializing table for group '{}'", blockGroup);
        
        // Collect blocks that meet the filter criteria and are not already used in other tables
        var blocks = ForgeRegistries.BLOCKS.getValues().stream().filter(block -> {
            BlockState state = block.defaultBlockState();
            return filterCriteria.test(block, state, level) && isUniqueToTables(block);
        }).toArray(Block[]::new);

        // Copy the list and shuffle it
        List<Block> blockList = new ArrayList<>(List.of(blocks));
        Collections.shuffle(blockList, random);

        // Merge the unshuffled and shuffled list to create a table of block pairs
        Map<Block, Block> table = new HashMap<>();
        for (int i = 0; i < blocks.length; i++) {
            table.put(blocks[i], blockList.get(i));
        }

        // Add the table to the table holder and save it to file
        blockGroups.put(blockGroup, table);
        saveTableAsJson(level, blockGroup);
    }

    // Helper method to check if a block is already used in any existing table
    private static boolean isUniqueToTables(Block block) {
        for (Map<Block, Block> table : blockGroups.values()) {
            if (table.containsKey(block)) {
                return false;
            }
        }
         return true;
    }

    // Filtering methods
    private static boolean isAirBlock(Block block, BlockState state, Level level) {
        return state.isAir();
    }
    private static boolean isFireBlock(Block block, BlockState state, Level level) {
        return block instanceof BaseFireBlock;
    }
    private static boolean isIndestructibleBlock(Block block, BlockState state, Level level) {
        return state.getDestroySpeed(level, BlockPos.ZERO) < 0;
    }
    private static boolean isEntityBlock(Block block, BlockState state, Level level) {
        return state.hasBlockEntity();
    }
    private static boolean isDelicateObjectBlock(Block block, BlockState state, Level level) {
        return (state.getDestroySpeed(level, BlockPos.ZERO) == 0);
    }
    private static boolean isAquaticBlock(Block block, BlockState state, Level level) {
        return state.getFluidState().isSource();
    }
    private static boolean isMenuBlock(Block block, BlockState state, Level level) {
        return state.getMenuProvider(level, BlockPos.ZERO) != null;
    }
    private static boolean isSlabBlock(Block block, BlockState state, Level level) {
        return (block instanceof SlabBlock);
    }
    private static boolean isStairBlock(Block block, BlockState state, Level level) {
        return (block instanceof StairBlock);
    }
    private static boolean isRedstoneComponentBlock(Block block, BlockState state, Level level) {
        return block.canConnectRedstone(state, level, BlockPos.ZERO, Direction.SOUTH);
    }
    private static boolean isProblemBlock(Block block, BlockState state, Level level) {
        return (block instanceof IceBlock) ||
               (block instanceof InfestedBlock) ||
               (block instanceof SpongeBlock) ||
               (block instanceof CarvedPumpkinBlock) ||
               state.is(Blocks.PISTON);
    }
    private static boolean isPlainBlock(Block block, BlockState state, Level level) {
        return state.isCollisionShapeFullBlock(level, BlockPos.ZERO);
    }        
    
    // Save any table as JSON
    private static void saveTableAsJson(Level level, String groupName) {
        Map<Block, Block> table = getTable(groupName);
        if (table.isEmpty()) {
            LOGGER.info("Block Shuffler: {} table is empty.", groupName);
            return;
        }

        // Create file and get plaintext names for the blocks
        File file = new File(((ServerLevel) level).getServer().getWorldPath(LevelResource.ROOT).toFile(), groupName + "_block_shuffle.json");
        Map<String, String> tableAsJson = new HashMap<>();
        table.forEach((key, value) -> {
            String keyName = ForgeRegistries.BLOCKS.getKey(key).toString();
            String valueName = ForgeRegistries.BLOCKS.getKey(value).toString();
            tableAsJson.put(keyName, valueName);
        });

        // Try to write the file to the disk
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(tableAsJson, writer);
            LOGGER.info("{} table saved successfully.", groupName);
        } catch (IOException e) {
            LOGGER.error("Failed to save {} data as JSON", groupName, e);
        }
    }

    // Load any table from JSON
    private static void loadTableFromJson(Level level, String groupName) {
        Map<Block, Block> table = new HashMap<>();
        File file = new File(((ServerLevel) level).getServer().getWorldPath(LevelResource.ROOT).toFile(), groupName + "_block_shuffle.json");
        if (!file.exists()) {
            LOGGER.info("No {} JSON file found.", groupName);
            return;
        }

        // Try to read the file and write it to a table
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> tableAsJson = GSON.fromJson(reader, type);

            // Convert back from registry names to Block instances
            tableAsJson.forEach((keyName, valueName) -> {
                Block key = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(keyName));
                Block value = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(valueName));

                if (key != null && value != null) {
                    table.put(key, value);
                } else {
                    LOGGER.warn("Failed to load blocks for key: {} or value: {}", keyName, valueName);
                }
            });

            blockGroups.put(groupName, table);
            LOGGER.info("{} table loaded successfully.", groupName);
        } catch (IOException e) {
            LOGGER.error("Failed to load {} data from JSON", groupName, e);
        }
    }

    // Custom predicate used to pass three parameters to the filter methods
    @FunctionalInterface
    private interface TriPredicate<T, U, V> {
        boolean test(T t, U u, V v);
    }
}
