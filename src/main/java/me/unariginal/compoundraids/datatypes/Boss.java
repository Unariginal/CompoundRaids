package me.unariginal.compoundraids.datatypes;

import com.cobblemon.mod.common.pokemon.Pokemon;

import java.util.ArrayList;
import java.util.Map;

public record Boss(String bossName, Pokemon bossPokemon, ArrayList<String> spawnLocations, Map<String, Double> weights) {
}
