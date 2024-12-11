package me.unariginal.compoundraids;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import kotlin.Unit;
import me.unariginal.compoundraids.commands.RaidCommands;
import me.unariginal.compoundraids.config.Config;
import me.unariginal.compoundraids.datatypes.BossBarData;
import me.unariginal.compoundraids.datatypes.Raid;
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
        LOGGER.info("[RAIDS] Loading Mod...");

        instance = this;
        new RaidCommands();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            mcServer = server;
            audiences = FabricServerAudiences.of(server);
            config = new Config();

            new DamageHandler();
            CobblemonEvents.POKEMON_ENTITY_SAVE_TO_WORLD.subscribe(Priority.NORMAL, event -> {
                activeRaids.forEach(raid -> {
                    if (event.getPokemonEntity().getUuid() == raid.getUuid()) {
                        event.cancel();
                    }
                });

                return Unit.INSTANCE;
            });
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
            } catch (Exception e) {
                e.printStackTrace();
            }

            ArrayList<Raid> markForDeletion = new ArrayList<>();
            for (int raidIndex = 0; raidIndex < activeRaids.size(); raidIndex++) {
                Raid raid = activeRaids.get(raidIndex);
                BossBarData bossBarData = raid.getBossBarData();

                if (raid.getStage() == 2) {
                    if (raid.getBoss().hp() <= 0 || raid.getBossEntity().isDead()) {
                        raid.handleBossDefeat();
                    } else {
                        if (raid.getBossEntity().isBattling()) {
                            if (raid.getBoss().bossPokemon().getCurrentHealth() != raid.getBoss().hp()) {
                                LOGGER.info("Fixing the health!!");
                                raid.getBoss().setHp(raid.getBoss().bossPokemon().getCurrentHealth());
                            }
                        }

                        if (bossBarData != null) {
                            float maxhp = raid.getBoss().maxhp();
                            float hp = raid.getBoss().hp();
                            if (maxhp > 0) {
                                float progress = hp / maxhp;
                                if (progress < 0) {
                                    progress = 0;
                                }
                                raid.getBar().progress(progress);
                            }
                        }
                    }
                } else if (raid.getStage() == -1) {
                    raid.stopRaid();
                    markForDeletion.add(raid);
                } else {
                    if (bossBarData != null) {
                        long tickDifference = bossBarData.endTick() - bossBarData.startTick();
                        float progressRate = 1 / (float)(tickDifference);
                        float newProgress = raid.getBar().progress() - progressRate;
                        raid.getBar().progress(newProgress >= 0 ? newProgress : 0);
                    }
                }

                mcServer.getPlayerManager().getPlayerList().forEach(raid::displayOverlay);
            }
            for (Raid r : markForDeletion) {
                activeRaids.remove(r);
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
