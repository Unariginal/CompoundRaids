package me.unariginal.compoundraids;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import kotlin.Unit;
import me.unariginal.compoundraids.commands.RaidCommands;
import me.unariginal.compoundraids.config.Config;
import me.unariginal.compoundraids.datatypes.BossBarData;
import me.unariginal.compoundraids.datatypes.Raid;
import me.unariginal.compoundraids.managers.BossBattleHandler;
import me.unariginal.compoundraids.managers.DamageHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
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
        // Register the /raid command
        new RaidCommands();

        // Handle events directly after the server finishes loading
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            mcServer = server;
            audiences = FabricServerAudiences.of(server);
            config = new Config();

            // Handle updating the health of the boss in battles
            new DamageHandler();

            // Cancels Pokemon Save event if the pokemon is the raid boss.
            // The server crashes if it tries to save a pokemon above level 100.
            CobblemonEvents.POKEMON_ENTITY_SAVE_TO_WORLD.subscribe(Priority.NORMAL, event -> {
                activeRaids.forEach(raid -> {
                    if (event.getPokemonEntity().getUuid() == raid.getUuid()) {
                        event.cancel();
                    }

                    raid.clones.forEach(entity -> {
                        if (event.getPokemonEntity().getUuid() == entity.getUuid()) {
                            event.cancel();
                        }
                    });
                });

                return Unit.INSTANCE;
            });

            CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.NORMAL, event -> {
                new BossBattleHandler().checkBossBattle(event);
                return Unit.INSTANCE;
            });
        });

        // Executes at the end of every server tick, hopefully 1/20th of a second.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Progress the raid phase when the timer runs out
            executeTasks();
            // Handle various status changes of the boss
            handleBossStatus();
            // Keep the boss in place if it gets pushed around
            keepBossInPlace();

            new BossBattleHandler().invokeBattles();
        });

        // Stop all raids when the server stops. This will be changed to include saving the boss to respawn when the server starts again.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            activeRaids.forEach(Raid::stopRaid);
            activeRaids.clear();
        });
    }

    private void executeTasks() {
        ServerWorld world = mcServer.getWorlds().iterator().next();
        long currentTick = world.getTime();
        try {
            activeRaids.forEach(raid -> {
                if (!raid.getTasks().isEmpty()) {
                    if (raid.getTasks().get(currentTick) != null) {
                        if (!raid.getTasks().get(currentTick).isEmpty()) {
                            // Run tasks that are supposed to run on this tick
                            raid.getTasks().get(currentTick).forEach(task -> task.action().run());
                            raid.getTasks().remove(currentTick);
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleBossStatus() {
        ArrayList<Raid> markForDeletion = new ArrayList<>();    // Raids to be deleted (can't delete raids while looping through the raids duh)

        for (Raid raid : activeRaids) {
            BossBarData bossBarData = raid.getBossBarData();

            if (raid.getStage() == 2) { // Fight stage
                if (raid.getBoss().hp() <= 0 || raid.getBossEntity().isDead()) {    // Boss is dead?
                    raid.handleBossDefeat();    // The boss was defeated!
                } else {    // Boss is alive
                    if (!raid.clones.isEmpty()) {    // If the boss is in a battle, the HP and boss health should be the same
                        raid.clones.forEach(entity -> {
                            if (entity.getPokemon().getCurrentHealth() != raid.getBoss().hp()) {
                                raid.getBoss().setHp(entity.getPokemon().getCurrentHealth());
                            }
                        });
                        raid.clones.forEach(entity -> {
                            if (entity.getPokemon().getCurrentHealth() != raid.getBoss().hp()) {
                                try {
                                    Field pokeField = entity.getPokemon().getClass().getDeclaredField("currentHealth");
                                    pokeField.setAccessible(true);
                                    pokeField.set(entity.getPokemon(), raid.getBoss().hp());
                                } catch (IllegalAccessException | NoSuchFieldException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }

                    if (bossBarData != null) {
                        float maxhp = raid.getBoss().maxhp();
                        float hp = raid.getBoss().hp();
                        if (maxhp > 0) {
                            float progress = hp / maxhp;
                            if (progress < 0) {
                                progress = 0;
                            }
                            for (ServerPlayerEntity player : mcServer.getPlayerManager().getPlayerList()) {
                                raid.getBar().get(player).progress(progress);   // Update the bossbar progress to match the boss HP
                            }
                        }
                    }
                }
            } else if (raid.getStage() == -1) { // Ready for removal
                raid.stopRaid();    // Stop the raid
                markForDeletion.add(raid); // Mark it for deletion
            } else {    // Other stages
                if (bossBarData != null) {
                    for (ServerPlayerEntity player : mcServer.getPlayerManager().getPlayerList()) {
                        long tickDifference = bossBarData.endTick() - bossBarData.startTick();
                        float progressRate = 1 / (float) (tickDifference);
                        float newProgress = raid.getBar().get(player).progress() - progressRate;
                        raid.getBar().get(player).progress(newProgress >= 0 ? newProgress : 0); // Update progress to match the count-down timer
                    }
                }
            }

            mcServer.getPlayerManager().getPlayerList().forEach(raid::displayOverlay);  // Update the actionbar for players
        }
        for (Raid r : markForDeletion) {    // Delete raids marked for deletion
            activeRaids.remove(r);
        }
    }

    private void keepBossInPlace() {
        activeRaids.forEach(raid -> {
            if (raid.getBossEntity().getPos().x != raid.getPosition().x ||
                raid.getBossEntity().getPos().y != raid.getPosition().y ||
                raid.getBossEntity().getPos().z != raid.getPosition().z) {

                raid.getBossEntity().teleport(raid.getPosition().x, raid.getPosition().y, raid.getPosition().z, false);
            }
        });
    }

    public static CompoundRaids getInstance() {
        return instance;
    }
}
