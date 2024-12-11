package me.unariginal.compoundraids.datatypes;

import com.cobblemon.mod.common.pokemon.Pokemon;

import java.util.ArrayList;
import java.util.Map;

public class Boss {
    private final String bossName;
    private final Pokemon bossPokemon;
    private int hp;
    private final int maxhp;
    private final ArrayList<String> spawnLocation;
    private final Map<String, Double> weights;

    public Boss(String bossName, Pokemon bossPokemon, int hp, int maxhp, ArrayList<String> spawnLocations, Map<String, Double> weights) {
        this.bossName = bossName;
        this.bossPokemon = bossPokemon;
        this.hp = hp;
        this.maxhp = maxhp;
        this.spawnLocation = spawnLocations;
        this.weights = weights;
    }

    public int hp() {
        return hp;
    }

    public ArrayList<String> spawnLocations() {
        return spawnLocation;
    }

    public int maxhp() {
        return maxhp;
    }

    public Pokemon bossPokemon() {
        return bossPokemon;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public String bossName() {
        return bossName;
    }

    public Map<String, Double> weights() {
        return weights;
    }
}
