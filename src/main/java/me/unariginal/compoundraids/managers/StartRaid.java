package me.unariginal.compoundraids.managers;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.unariginal.compoundraids.CompoundRaids;
import me.unariginal.compoundraids.datatypes.Boss;
import me.unariginal.compoundraids.datatypes.Location;
import me.unariginal.compoundraids.datatypes.Raid;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Random;

public class StartRaid {
    public static CompoundRaids cr = CompoundRaids.getInstance();

    public static int start(CommandContext<ServerCommandSource> ctx) {
        Messages messages = cr.config.getMessagesObject();
        String boss = StringArgumentType.getString(ctx, "boss");

        Pokemon bossPokemon;
        ArrayList<String> spawnLocations;

        Boss bossInfo = cr.config.getBossList().get(boss);
        bossPokemon = bossInfo.bossPokemon();
        spawnLocations = bossInfo.spawnLocations();

        // Select a spawn location based on the weights set *****
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
        // *******************************************************

        if (chosenLocation == null) {
            CompoundRaids.LOGGER.info("[RAIDS] The chosen location was NULL");
            return 0;
        }

        // Get and set the spawn location of the Pokemon *****
        String locationKey = spawnLocations.get(spawnLocations.indexOf(chosenLocation));

        if (locationKey == null) {
            CompoundRaids.LOGGER.info("[RAIDS] Location String was NULL");
            return 0;
        }

        Location location = cr.config.getLocationList().get(locationKey);
        Vec3d position = location.coordinates();
        ServerWorld world = location.world();
        // ***************************************************

        if (bossPokemon == null) {
            CompoundRaids.LOGGER.info("[RAIDS] Boss Pokemon was NULL");
            return 0;
        }

        //CompoundRaids.LOGGER.info("[RAIDS] Selected {}", bossPokemon.getSpecies());

        PokemonEntity entity = new PokemonEntity(world, bossPokemon, CobblemonEntities.POKEMON);
        entity.setPosition(position);

        Raid newRaid = new Raid(entity.getUuid(), bossInfo, entity, world);
        cr.activeRaids.add(newRaid);

        int id = -1;
        for (int i = 0; i < cr.activeRaids.size(); i++) {
            if (cr.activeRaids.get(i).getUuid() == newRaid.getUuid()) {
                id = i;
            }
        }

        if (id != -1) {
            cr.activeRaids.get(id).beginPrePhase();
        }

        return 1;
    }
}
