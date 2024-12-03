package me.unariginal.compoundraids.managers;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import kotlin.Unit;
import me.unariginal.compoundraids.CompoundRaids;

public class DamageHandler {
    public DamageHandler() {
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, event -> {
           PokemonBattle battle = event.getBattle();
           int damage = 0;
           if (battle.isPvW()) {
               battle.getActors().forEach(actor -> {
                   if (actor.getType() == ActorType.WILD) {
                       BattlePokemon wildPokemon = actor.getPokemonList().getFirst();
                       CompoundRaids.getInstance().activeRaids.forEach(raid -> {
                           if (wildPokemon.getUuid() == raid.getUuid()) {
                               if (wildPokemon.getHealth() <= 0) {
                                   raid.handleBossDefeat();
                               }
                           }
                       });
                   }
               });
           }
           return Unit.INSTANCE;
        });
    }
}
