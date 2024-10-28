package com.corwin.blockrandomizer;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;


@Mod.EventBusSubscriber(modid = BlockRandomizer.MODID)
public class StartToolHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        LOGGER.info("Login");
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Login Server"); 
            ensureTools(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LOGGER.info("Respawn");
        if (event.getEntity() instanceof ServerPlayer player) {
            LOGGER.info("Respawn Server");
            ensureTools(player);
        }
    }

    private static void ensureTools(ServerPlayer player) {
        LOGGER.info("Ensuring Tools");
        ensureTool(player, "pickaxe");
        ensureTool(player, "axe");
        ensureTool(player, "shovel");
        ensureTool(player, "hoe");
    }

    private static void ensureTool(ServerPlayer player, String tool) {
        LOGGER.info("Ensuring {}", tool);
        if (hasTool(player, tool)) return; 
        ItemStack unbreakableTool = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft:netherite_" + tool)));
        unbreakableTool.getOrCreateTag().putBoolean("Unbreakable", true);
        player.getInventory().add(unbreakableTool);
    }
    
    // Method to check if a player has any pickaxe in their inventory
    public static boolean hasTool(ServerPlayer player, String tool) {
        LOGGER.info("Checking for {}", tool);
        for (ItemStack itemStack : player.getInventory().items) {
            if (isTool(itemStack, tool)) {
                return true;
            }
        }
        return false;
    }

    // Helper method to determine if the item is a pickaxe
    private static boolean isTool(ItemStack itemStack, String tool) {
        LOGGER.info("Checking {} for {}", itemStack, tool);
        if (itemStack == null) return false;
        return isItemAToolOfMaterial(itemStack, tool, "wooden") ||
               isItemAToolOfMaterial(itemStack, tool, "stone") ||
               isItemAToolOfMaterial(itemStack, tool, "gold") ||
               isItemAToolOfMaterial(itemStack, tool, "iron") ||
               isItemAToolOfMaterial(itemStack, tool, "diamond") ||
               isItemAToolOfMaterial(itemStack, tool, "netherite");

    }

    private static boolean isItemAToolOfMaterial(ItemStack itemStack, String tool, String material) {
        LOGGER.info("Checking {} for {}_{}", itemStack, material, tool);
        return itemStack.is(ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft", material + "_" + tool)));
    }
}
