package me.unariginal.compoundraids.config;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.unariginal.compoundraids.CompoundRaids;
import me.unariginal.compoundraids.datatypes.Boss;
import me.unariginal.compoundraids.datatypes.Location;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class Config {
    private final Map<String, Boss> bossList = new HashMap<>();
    private final Map<String, Location> locationList = new HashMap<>();

    public Config() {
        try {
            checkFiles();
        } catch (IOException e) {
            e.printStackTrace();
            CompoundRaids.LOGGER.error("Failed to generate default config file.");
        }
        loadBosses();
        loadLocations();
    }

    public void checkFiles() throws IOException {
        Path folderPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids");
        File folderFile = folderPath.toFile();
        if (!folderFile.exists()) {
            try {
                folderFile.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/config.json");
        File configFile = configPath.toFile();

        if (!configFile.exists()) {
            configFile.createNewFile();

            InputStream in = CompoundRaids.class.getResourceAsStream("/config.json");
            OutputStream out = new FileOutputStream(configFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }

        Path bossesPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/bosses.json");
        File bossesFile = bossesPath.toFile();

        if (!bossesFile.exists()) {
            bossesFile.createNewFile();

            InputStream in = CompoundRaids.class.getResourceAsStream("/bosses.json");
            OutputStream out = new FileOutputStream(bossesFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }

        Path locationsPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/locations.json");
        File locationsFile = locationsPath.toFile();

        if (!locationsFile.exists()) {
            locationsFile.createNewFile();

            InputStream in = CompoundRaids.class.getResourceAsStream("/locations.json");
            OutputStream out = new FileOutputStream(locationsFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }
    }

    public void loadBosses() {
        Path bossesPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/bosses.json");
        File bossesFile = bossesPath.toFile();

        JsonElement root;
        try {
            root = JsonParser.parseReader(new FileReader(bossesFile));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        JsonObject bossesObj = root.getAsJsonObject();
        CompoundRaids.LOGGER.info("[RAIDS] Loading the bosses...");

        for (String boss : bossesObj.keySet()) {
            JsonObject bossObj = bossesObj.get(boss).getAsJsonObject();

            Species bossSpecies = PokemonSpecies.INSTANCE.getByName(bossObj.get("species").getAsString());
            if (bossSpecies != null) {
                Pokemon bossPokemon = bossSpecies.create(bossObj.get("level").getAsInt());
                bossPokemon.getFeatures().clear();
                PokemonProperties.Companion.parse(bossObj.get("form").getAsString()).apply(bossPokemon);
                bossPokemon.setScaleModifier(bossObj.get("scale").getAsFloat());

                switch (bossObj.get("gender").getAsString().toLowerCase()) {
                    case "male":
                        bossPokemon.setGender(Gender.MALE);
                        break;
                    case "female":
                        bossPokemon.setGender(Gender.FEMALE);
                        break;
                    case "genderless":
                        bossPokemon.setGender(Gender.GENDERLESS);
                        break;
                }

                bossPokemon.updateAbility(Abilities.INSTANCE.getOrException(bossObj.get("ability").getAsString()).create(false, Priority.LOWEST));
                bossPokemon.setShiny(bossObj.get("shiny").getAsBoolean());
                bossPokemon.setNature(Objects.requireNonNull(Natures.INSTANCE.getNature(bossObj.get("nature").getAsString())));

                bossPokemon.setEV(Stats.HP, bossObj.get("evs").getAsJsonObject().get("hp").getAsInt());
                bossPokemon.setEV(Stats.ATTACK, bossObj.get("evs").getAsJsonObject().get("attack").getAsInt());
                bossPokemon.setEV(Stats.DEFENCE, bossObj.get("evs").getAsJsonObject().get("defence").getAsInt());
                bossPokemon.setEV(Stats.SPECIAL_ATTACK, bossObj.get("evs").getAsJsonObject().get("special_attack").getAsInt());
                bossPokemon.setEV(Stats.SPECIAL_DEFENCE, bossObj.get("evs").getAsJsonObject().get("special_defence").getAsInt());
                bossPokemon.setEV(Stats.SPEED, bossObj.get("evs").getAsJsonObject().get("speed").getAsInt());

                bossPokemon.setIV(Stats.HP, bossObj.get("ivs").getAsJsonObject().get("hp").getAsInt());
                bossPokemon.setIV(Stats.ATTACK, bossObj.get("ivs").getAsJsonObject().get("attack").getAsInt());
                bossPokemon.setIV(Stats.DEFENCE, bossObj.get("ivs").getAsJsonObject().get("defence").getAsInt());
                bossPokemon.setIV(Stats.SPECIAL_ATTACK, bossObj.get("ivs").getAsJsonObject().get("special_attack").getAsInt());
                bossPokemon.setIV(Stats.SPECIAL_DEFENCE, bossObj.get("ivs").getAsJsonObject().get("special_defence").getAsInt());
                bossPokemon.setIV(Stats.SPEED, bossObj.get("ivs").getAsJsonObject().get("speed").getAsInt());

                MoveTemplate move1 = Moves.INSTANCE.getByName(bossObj.get("moves").getAsJsonArray().get(0).getAsString());
                MoveTemplate move2 = Moves.INSTANCE.getByName(bossObj.get("moves").getAsJsonArray().get(1).getAsString());
                MoveTemplate move3 = Moves.INSTANCE.getByName(bossObj.get("moves").getAsJsonArray().get(2).getAsString());
                MoveTemplate move4 = Moves.INSTANCE.getByName(bossObj.get("moves").getAsJsonArray().get(3).getAsString());

                if (move1 != null && move2 != null && move3 != null & move4 != null) {
                    bossPokemon.getMoveSet().setMove(0, move1.create());
                    bossPokemon.getMoveSet().setMove(1, move2.create());
                    bossPokemon.getMoveSet().setMove(2, move3.create());
                    bossPokemon.getMoveSet().setMove(3, move4.create());
                }

                bossPokemon.getCustomProperties().add(UncatchableProperty.INSTANCE.uncatchable());

                // TODO: Held Item

                ArrayList<String> spawnLocations = new ArrayList<>();
                Map<String, Double> weights = new HashMap<>();
                for (JsonElement element : bossObj.get("locations").getAsJsonArray().asList()) {
                    String locStr = element.getAsJsonObject().get("location").getAsString();
                    Double locWeight = element.getAsJsonObject().get("weight").getAsDouble();

                    spawnLocations.add(locStr);
                    weights.put(locStr, locWeight);
                }

                Boss bossInfo = new Boss(boss, bossPokemon, spawnLocations, weights);

                bossList.put(boss, bossInfo);

                CompoundRaids.LOGGER.info("[RAIDS] Created Boss {}. Species is {}", boss, bossSpecies);
            }
        }
        CompoundRaids.LOGGER.info("[RAIDS] Loaded {} bosses!", bossList.size());
    }

    public void loadLocations() {
        Path locationsPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/locations.json");
        File locationsFile = locationsPath.toFile();

        JsonElement root;
        try {
            root = JsonParser.parseReader(new FileReader(locationsFile));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        JsonObject locationsObj = root.getAsJsonObject();
        CompoundRaids.LOGGER.info("[RAIDS] Loading the locations...");

        for (String location : locationsObj.keySet()) {
            JsonObject locObj = locationsObj.get(location).getAsJsonObject();

            double x = locObj.get("xPos").getAsDouble();
            double y = locObj.get("yPos").getAsDouble();
            double z = locObj.get("zPos").getAsDouble();

            Vec3d coords = new Vec3d(x, y, z);
            String worldStr = locObj.get("world").getAsString();
            ServerWorld theWorld = CompoundRaids.getInstance().mcServer.getOverworld();

            for (ServerWorld world : CompoundRaids.instance.mcServer.getWorlds()) {
                if (world.asString().equalsIgnoreCase(worldStr)) {
                    theWorld = world;
                    break;
                }
            }

            Location loc = new Location(coords, theWorld);
            locationList.put(location, loc);

            CompoundRaids.LOGGER.info("[RAIDS] Created location {}.", location);
        }

        CompoundRaids.LOGGER.info("[RAIDS] Loaded {} locations!", locationList.size());
    }

    public Map<String, Boss> getBossList() {
        return bossList;
    }

    public Map<String, Location> getLocationList() {
        return locationList;
    }
}