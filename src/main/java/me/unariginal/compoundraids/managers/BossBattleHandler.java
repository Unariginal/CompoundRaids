package me.unariginal.compoundraids.managers;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.battles.BattleStartedPreEvent;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import me.unariginal.compoundraids.CompoundRaids;
import me.unariginal.compoundraids.utils.PokemonUtils;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BossBattleHandler {
    public void checkBossBattle(BattleStartedPreEvent event) {
        CompoundRaids.getInstance().activeRaids.forEach(raid -> {
            if (raid.getStage() == 2) {
                boolean isBossBattle = false;
                ServerPlayerEntity player = null;
                PlayerPartyStore party = null;
                for (BattleActor actor : event.getBattle().getActors()) {
                    if (actor instanceof PlayerBattleActor) {
                        player = ((PlayerBattleActor) actor).getEntity();
                        if (player != null) {
                            party = Cobblemon.INSTANCE.getStorage().getParty(player);
                        }
                    } else if (actor instanceof PokemonBattleActor) {
                        for (BattlePokemon pokemon : actor.getPokemonList()) {
                            if (pokemon.getEntity() != null) {
                                if (pokemon.getEntity().getUuid() == raid.getUuid()) {
                                    CompoundRaids.LOGGER.info("Battle Attempted and Cancelled.");
                                    isBossBattle = true;
                                }
                            }
                        }
                    }
                }

                if (isBossBattle && player != null && party.size() > 0) {
                    // TODO: RAID PASSES HERE
                    // TODO: CHECK MAX PLAYERS
                    // TODO: COOLDOWN(?)
                    // TODO: CHECK BANNED LIST
                    raid.playersToBeBattled.add(player);
                    event.cancel();
                }
            }
        });
    }

    public void invokeBattles() {
        CompoundRaids.getInstance().activeRaids.forEach(raid -> {
            if (raid.getStage() == 2) {
                for (ServerPlayerEntity player : raid.playersToBeBattled) {
                    if (!raid.playersBattling.contains(player)) {
                        Pokemon bossClone = PokemonUtils.clone(raid.getBoss().bossPokemon());
                        bossClone.getCustomProperties().add(UncatchableProperty.INSTANCE.uncatchable());

                        Vec3d position = new Vec3d(player.getPos().x + ((Math.abs(raid.getPosition().x) - Math.abs(player.getPos().x)) * 1.5), player.getPos().y, player.getPos().z + ((Math.abs(raid.getPosition().z) - Math.abs(player.getPos().z)) * 1.5));
                        ServerWorld world = player.getServerWorld();

                        PokemonEntity entity = new PokemonEntity(world, bossClone, CobblemonEntities.POKEMON);
                        entity.setPosition(position);
                        world.spawnEntity(entity);
                        raid.clones.add(entity);

                        //entity.setAiDisabled(true);
                        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 9999, false, false));
                        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9999, false, false));

                        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
                        UUID finalLeadingPokemon = PokemonUtils.getLeadingPokemon(party);
                        raid.addTask(world, 1, () -> {
                            BattleBuilder.INSTANCE.pve(player, entity, finalLeadingPokemon, BattleFormat.Companion.getGEN_9_SINGLES(), false, true);
                            raid.playersBattling.add(player);
                        });
                    }
                }

                raid.playersToBeBattled.clear();
            }
        });
    }
}
