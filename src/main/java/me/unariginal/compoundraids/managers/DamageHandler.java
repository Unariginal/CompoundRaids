package me.unariginal.compoundraids.managers;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import me.unariginal.compoundraids.CompoundRaids;

import java.lang.reflect.Field;

public class DamageHandler {
    public DamageHandler() {
        CobblemonEvents.BATTLE_STARTED_POST.subscribe(Priority.NORMAL, event -> {
            PokemonBattle battle = event.getBattle();
            battle.getActors().forEach(actor -> {
               switch (actor.getType()) {
                   case ActorType.WILD:
                       CompoundRaids.LOGGER.info("wild actor");
                       break;
                   case ActorType.NPC:
                       CompoundRaids.LOGGER.info("NPC actor");
                       break;
                   case ActorType.PLAYER:
                       CompoundRaids.LOGGER.info("Player actor");
                       break;
               }

               if (actor.getType() == ActorType.WILD) {
                   BattlePokemon wildPokemon = actor.getPokemonList().getFirst();
                   PokemonEntity pokemonEntity = wildPokemon.getEntity();
                   Pokemon pokemon = wildPokemon.getEffectedPokemon();
                   if (pokemonEntity != null) {
                       if (pokemonEntity.getUuid() != null) {
                           CompoundRaids.getInstance().activeRaids.forEach(raid -> {
                               if (pokemonEntity.getUuid() == raid.getUuid()) {
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
                       }
                   }
               }
            });
            return Unit.INSTANCE;
        });
    }
}
