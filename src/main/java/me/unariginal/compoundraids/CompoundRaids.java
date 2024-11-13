package me.unariginal.compoundraids;

import me.unariginal.compoundraids.commands.RaidCommands;
import me.unariginal.compoundraids.config.Config;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompoundRaids implements ModInitializer {
    public static final String MOD_ID = "compoundraids";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static MiniMessage mm = MiniMessage.miniMessage();
    public static FabricServerAudiences audiences;

    public static CompoundRaids instance;
    public Config config;
    public MinecraftServer mcServer;

    @Override
    public void onInitialize() {
        instance = this;
        new RaidCommands();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            mcServer = server;
            audiences = FabricServerAudiences.of(server);
            config = new Config();
        });
    }

    public static CompoundRaids getInstance() {
        return instance;
    }
}
