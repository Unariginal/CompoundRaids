package me.unariginal.compoundraids.commands;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.compoundraids.CompoundRaids;
import me.unariginal.compoundraids.config.Config;
import me.unariginal.compoundraids.datatypes.Boss;
import me.unariginal.compoundraids.datatypes.Location;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Random;

public class RaidCommands {
    public RaidCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("raid")
                            .then(
                                    CommandManager.literal("reload")
                                            .requires(Permissions.require("cc.raids.reload", 4))
                                            .executes(this::reload)
                            )
                            .then(
                                    CommandManager.literal("start")
                                            .requires(Permissions.require("cc.raids.start", 4))
                                            .then(
                                                    CommandManager.argument("boss", StringArgumentType.string())
                                                            .suggests(new BossSuggestions())
                                                            .executes(this::start)
                                            )
                            )
                            .then(
                                    CommandManager.literal("stop")
                                            .requires(Permissions.require("cc.raids.stop", 4))
                                            .executes(this::stop)
                            )
                            .then(
                                    CommandManager.literal("give")
                                            .requires(Permissions.require("cc.raids.give",4))
                                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                                    .then(CommandManager.argument("item", StringArgumentType.string())
                                                            .suggests(new ItemSuggestions())
                                                            .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                                                                    .executes(this::give)
                                                            )
                                                    )
                                            )
                            )
            );
        });
    }

    private int reload(CommandContext<ServerCommandSource> ctx) {
        CompoundRaids.getInstance().config = new Config();
        return 1;
    }

    private int start(CommandContext<ServerCommandSource> ctx) {
        String boss = StringArgumentType.getString(ctx, "boss");

        Pokemon bossPokemon;
        ArrayList<String> spawnLocations;

        Boss bossInfo = CompoundRaids.getInstance().config.getBossList().get(boss);
        bossPokemon = bossInfo.bossPokemon();
        spawnLocations = bossInfo.spawnLocations();

        Random rand = new Random();
        double totalWeight = 0.0;
        for (String location : spawnLocations) {
            totalWeight += bossInfo.weights().get(location);
        }

        double randWeight = rand.nextDouble(totalWeight);
        double cumulativeWeight = 0.0;
        String chosenLocation = null;

        for (String location : spawnLocations) {
            cumulativeWeight += bossInfo.weights().get(location);
            if (randWeight < cumulativeWeight) {
                chosenLocation = location;
                break;
            }
        }
        if (chosenLocation == null) {
            CompoundRaids.LOGGER.info("[RAIDS] The chosen location was NULL");
            return 1;
        }

        String locationKey = spawnLocations.get(spawnLocations.indexOf(chosenLocation));

        if (locationKey == null) {
            CompoundRaids.LOGGER.info("[RAIDS] Location String was NULL");
            return 1;
        }

        Location location = CompoundRaids.getInstance().config.getLocationList().get(locationKey);
        Vec3d position = location.coordinates();
        ServerWorld world = location.world();

        CompoundRaids.LOGGER.info("[RAIDS] Selected {}", bossPokemon);

        if (bossPokemon == null) {
            CompoundRaids.LOGGER.info("[RAIDS] Boss Pokemon was NULL");
            return 1;
        }

        var entity = new PokemonEntity(world, bossPokemon, CobblemonEntities.POKEMON);
        entity.setPosition(position);

        int chunkX = (int) Math.floor(position.getX() / 16);
        int chunkZ = (int) Math.floor(position.getZ() / 16);

        world.setChunkForced(chunkX, chunkZ, true);

        world.spawnEntity(entity);

        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9999, false, false));
        entity.setPersistent();

        world.setChunkForced(chunkX, chunkZ, false);

        CompoundRaids.LOGGER.info("[RAIDS] {} was spawned", bossPokemon.getSpecies());
        CompoundRaids.LOGGER.info("[RAIDS] Raid Started!");
        return 1;
    }

    private int stop(CommandContext<ServerCommandSource> ctx) {
        return 1;
    }

    private int give(CommandContext<ServerCommandSource> ctx) {
        return 1;
    }
}
