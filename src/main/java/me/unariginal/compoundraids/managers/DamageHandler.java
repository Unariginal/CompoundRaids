package me.unariginal.compoundraids.managers;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import me.unariginal.compoundraids.CompoundRaids;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class DamageHandler {
    public DamageHandler() {
        CobblemonEvents.BATTLE_STARTED_POST.subscribe(Priority.NORMAL, event -> {
            PokemonBattle battle = event.getBattle();
            battle.getActors().forEach(actor -> {
               if (actor instanceof PokemonBattleActor) {
                   BattlePokemon wildPokemon = actor.getPokemonList().getFirst();
                   PokemonEntity pokemonEntity = wildPokemon.getEntity();
                   Pokemon pokemon = wildPokemon.getEffectedPokemon();
                   if (pokemonEntity != null) {
                       if (pokemonEntity.getUuid() != null) {
                           CompoundRaids.getInstance().activeRaids.forEach(raid -> {
                               raid.clones.forEach(entity -> {
                                   if (pokemonEntity.getUuid() == entity.getUuid()) {
                                       if (pokemon.getCurrentHealth() > raid.getBoss().hp()) {
                                           try {
                                               Field pokeField = pokemon.getClass().getDeclaredField("currentHealth");
                                               pokeField.setAccessible(true);
                                               pokeField.set(pokemon, raid.getBoss().hp());
                                           } catch (IllegalAccessException | NoSuchFieldException e) {
                                               throw new RuntimeException(e);
                                           }
                                       }
                                   }
                               });
                           });
                       }
                   }
               }
            });
            return Unit.INSTANCE;
        });

        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
            for (BattleActor actor : event.getWinners()) {
                if (actor instanceof PokemonBattleActor) {
                    BattlePokemon wildPokemon = actor.getPokemonList().getFirst();
                    PokemonEntity pokemonEntity = wildPokemon.getEntity();
                    if (pokemonEntity != null) {
                        if (pokemonEntity.getUuid() != null) {
                            CompoundRaids.getInstance().activeRaids.forEach(raid -> {
                                ArrayList<PokemonEntity> toRemove = new ArrayList<>();
                                raid.clones.forEach(clone -> {
                                    if (pokemonEntity.getUuid() == clone.getUuid()) {
                                        clone.kill();
                                        toRemove.add(clone);
                                    }
                                });
                                toRemove.forEach(entity -> raid.clones.remove(entity));
                                toRemove.clear();
                            });
                        }
                    }
                }
            }
            return Unit.INSTANCE;
        });
    }
}
