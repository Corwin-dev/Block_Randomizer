package com.corwin.blockrandomizer;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockRandomizer.MODID)
public class FireTickDisabler {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Get the overworld (main server world) instance
        ServerLevel serverLevel = event.getServer().overworld();
        
        // Set the game rule to disable fire spread and extinguishing
        serverLevel.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(false, serverLevel.getServer());
    }
}
