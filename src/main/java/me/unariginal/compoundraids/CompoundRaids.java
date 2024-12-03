package me.unariginal.compoundraids;

import me.unariginal.compoundraids.commands.RaidCommands;
import me.unariginal.compoundraids.config.Config;
import me.unariginal.compoundraids.datatypes.Raid;
import me.unariginal.compoundraids.datatypes.Task;
import me.unariginal.compoundraids.managers.DamageHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompoundRaids implements ModInitializer {
    private static final String MOD_ID = "compoundraids";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static FabricServerAudiences audiences;
    public MiniMessage mm = MiniMessage.miniMessage();

    private static CompoundRaids instance;
    public Config config;
    public MinecraftServer mcServer;
    public ArrayList<Raid> activeRaids = new ArrayList<>();

    @Override
    public void onInitialize() {
        instance = this;
        new RaidCommands();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            mcServer = server;
            audiences = FabricServerAudiences.of(server);
            config = new Config();

            new DamageHandler();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getWorlds().iterator().next();
            long currentTick = world.getTime();
            try {
                activeRaids.forEach(raid -> {
                    if (!raid.getTasks().isEmpty()) {
                        if (raid.getTasks().get(currentTick) != null) {
                            if (!raid.getTasks().get(currentTick).isEmpty()) {
                                raid.getTasks().get(currentTick).forEach(task -> task.action().run());
                                raid.getTasks().remove(currentTick);
                            }
                        }
                    }
                });
//                if (!tasks.isEmpty()) {
//                    //ArrayList<Long> markForDeletion = new ArrayList<>();
//                    //for (Long tickKey : tasks.keySet()) {
//                        //if (currentTick >= tickKey) {
//                    if (tasks.get(currentTick) != null) {
//                        if (!tasks.get(currentTick).isEmpty()) {
//                            tasks.get(currentTick).forEach(task -> task.action().run());
//                            tasks.remove(currentTick);
//                        }
//                    }
                        //}
                    //}
                    //for (Long l : markForDeletion) {
                    //    tasks.remove(l);
                    //}
                //}
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (currentTick % 20 == 0) {
                ArrayList<Raid> markForDeletion = new ArrayList<>();
                for (int raidIndex = 0; raidIndex < activeRaids.size(); raidIndex++) {
                    if (activeRaids.get(raidIndex).getStage() == -1) {
                        activeRaids.get(raidIndex).stopRaid();
                        markForDeletion.add(activeRaids.get(raidIndex));
                    }

                    if (activeRaids.get(raidIndex).getStage() == 2) {
                        if (activeRaids.get(raidIndex).getBossEntity().isDead()) {
                            activeRaids.get(raidIndex).handleBossDefeat();
                        }
                    }
                }
                for (Raid r : markForDeletion) {
                    activeRaids.remove(r);
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            activeRaids.forEach(Raid::stopRaid);
            activeRaids.clear();
        });
    }

    public static CompoundRaids getInstance() {
        return instance;
    }
}
