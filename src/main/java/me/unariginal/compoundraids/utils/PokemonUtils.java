package me.unariginal.compoundraids.utils;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.UUID;

public class PokemonUtils {
    public static Pokemon clone(Pokemon original) {
        Pokemon clone = new Pokemon();
        clone.setSpecies(original.getSpecies());

        try {
            Field pokeField = clone.getClass().getDeclaredField("level");
            pokeField.setAccessible(true);
            pokeField.set(clone, original.getLevel());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        clone.getCustomProperties().clear();
        PokemonProperties.Companion.parse(original.getForm().getName()).apply(clone);
        clone.setGender(original.getGender());
        clone.updateAbility(original.getAbility());
        clone.setShiny(original.getShiny());
        clone.setNature(original.getNature());
        clone.setEvs$common(original.getEvs());
        clone.setIvs$common(original.getIvs());
        clone.getMoveSet().copyFrom(original.getMoveSet());
        clone.heal();
        clone.setCurrentHealth(original.getCurrentHealth());

        return clone;
    }

    public static @Nullable UUID getLeadingPokemon(PlayerPartyStore party) {
        UUID leadingPokemon = null;
        for (Pokemon pokemon : party) {
            if (!pokemon.isFainted()) {
                leadingPokemon = pokemon.getUuid();
                break;
            }
        }

        // Start battle 1 tick later, it doesn't work if the battle starts the same tick that the entity is spawned in...
        return leadingPokemon;
    }
}
