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
import me.unariginal.compoundraids.datatypes.*;
import me.unariginal.compoundraids.managers.Bossbar;
import me.unariginal.compoundraids.managers.Messages;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.bossbar.BossBar;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

public class Config {
    private RaidSettings raidSettings;
    private final Map<String, Boss> bossList = new HashMap<>();
    private final Map<String, Location> locationList = new HashMap<>();
    private final Map<String, RewardPool> rewardPoolList = new HashMap<>();
    private final Map<String, Category> categoryList = new HashMap<>();
    private final Map<String, Bossbar> bossbarList = new HashMap<>();
    private Messages messagesObject;

    //private Item raidVoucherItem;
    //private Item raidPassItem;

    public Config() {
        try {
            checkFiles();
        } catch (IOException e) {
            e.printStackTrace();
            CompoundRaids.LOGGER.error("Failed to generate default config file.");
        }
        loadConfig();
        loadCategories();
        loadBosses();
        loadLocations();
        loadRewards();
        loadMessages();
        loadBossbars();
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

        Path rewardsPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/rewards.json");
        File rewardsFile = rewardsPath.toFile();

        if (!rewardsFile.exists()) {
            rewardsFile.createNewFile();

            InputStream in = CompoundRaids.class.getResourceAsStream("/rewards.json");
            OutputStream out = new FileOutputStream(rewardsFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }

        Path categoriesPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/categories.json");
        File categoriesFile = categoriesPath.toFile();

        if (!categoriesFile.exists()) {
            categoriesFile.createNewFile();

            InputStream in = CompoundRaids.class.getResourceAsStream("/categories.json");
            OutputStream out = new FileOutputStream(categoriesFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }

        Path messagesPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/messages.json");
        File messagesFile = messagesPath.toFile();

        if (!messagesFile.exists()) {
            messagesFile.createNewFile();

            InputStream in = CompoundRaids.class.getResourceAsStream("/messages.json");
            OutputStream out = new FileOutputStream(messagesFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            in.close();
            out.close();
        }
    }

    public void loadConfig() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/config.json");
        File configFile = configPath.toFile();

        JsonElement root;
        try {
            root = JsonParser.parseReader(new FileReader(configFile));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        JsonObject configObj = root.getAsJsonObject();
        CompoundRaids.LOGGER.info("[RAIDS] Loading the config...");

        String timezone = configObj.get("timezone").getAsString();
        JsonObject settings = configObj.getAsJsonObject("raid_settings");
        long pre = settings.get("raid_prePhaseTimeSeconds").getAsLong();
        long fight = settings.get("raid_fightPhaseTimeSeconds").getAsLong();
        long postFight = settings.get("raid_afterFightCooldownSeconds").getAsLong();
        long catchPhase = settings.get("raid_catchPhaseTimeSeconds").getAsLong();
        long catchWarning = settings.get("raid_catchWarningTimeSeconds").getAsLong();
        long healthInc = settings.get("raid_healthIncreasePerPlayer").getAsLong();
        raidSettings = new RaidSettings(timezone, pre, fight, postFight, catchPhase, catchWarning, healthInc);

        //raidVoucherItem = getItem(configObj.get("item_raidVoucher").getAsString());
        //raidPassItem = getItem(configObj.get("item_raidPass").getAsString());

        CompoundRaids.LOGGER.info("[RAIDS] Config loaded!");
    }

    public void loadCategories() {
        Path categoriesPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/categories.json");
        File categoriesFile = categoriesPath.toFile();

        JsonElement root;
        try {
            root = JsonParser.parseReader(new FileReader(categoriesFile));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        JsonObject categoriesObj = root.getAsJsonObject();
        CompoundRaids.LOGGER.info("[RAIDS] Loading categories...");

        for (String category : categoriesObj.keySet()) {
            JsonObject categoryObj = categoriesObj.getAsJsonObject(category);

            boolean isRandomRaid = categoryObj.get("enableRandomRaids").getAsBoolean();
            int minWait = categoryObj.get("random_minWaitTimeSeconds").getAsInt();
            int maxWait = categoryObj.get("random_maxWaitTimeSeconds").getAsInt();

            ArrayList<String> times = new ArrayList<>();
            for (JsonElement timeElement : categoryObj.getAsJsonArray("setRaidTimes").asList()) {
                String timeStr = timeElement.getAsString();
                times.add(timeStr);
            }

            int minPlayers = categoryObj.get("minimumPlayersToStart").getAsInt();
            boolean useVoucher = categoryObj.get("enableRaidVoucher").getAsBoolean();

            Category raidCategory = new Category(isRandomRaid, minWait, maxWait, times, minPlayers, useVoucher);
            categoryList.put(category, raidCategory);
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
                int level = bossObj.get("level").getAsInt();
                Pokemon bossPokemon = new Pokemon();
                bossPokemon.setSpecies(bossSpecies);

                try {
                    Field pokeField = bossPokemon.getClass().getDeclaredField("level");
                    pokeField.setAccessible(true);
                    pokeField.set(bossPokemon, level);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }

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

                int maxhp = (((2 * bossPokemon.getSpecies().getBaseStats().get(Stats.HP) + bossPokemon.getIvs().get(Stats.HP) + (bossPokemon.getEvs().get(Stats.HP) / 4)) * level) / 100) + level + 10;

                try {
                    Field pokeField = bossPokemon.getClass().getDeclaredField("currentHealth");
                    pokeField.setAccessible(true);
                    pokeField.set(bossPokemon, maxhp);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }

                bossPokemon.getCustomProperties().add(UncatchableProperty.INSTANCE.uncatchable());
                bossPokemon.getSpecies().getFeatures().add("dmax=10");

                // TODO: Held Item

                ArrayList<String> spawnLocations = new ArrayList<>();
                Map<String, Double> weights = new HashMap<>();
                for (JsonElement element : bossObj.get("locations").getAsJsonArray().asList()) {
                    String locStr = element.getAsJsonObject().get("location").getAsString();
                    Double locWeight = element.getAsJsonObject().get("weight").getAsDouble();

                    spawnLocations.add(locStr);
                    weights.put(locStr, locWeight);
                }

                Boss bossInfo = new Boss(boss, bossPokemon, maxhp, maxhp, spawnLocations, weights);

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

            for (ServerWorld world : CompoundRaids.getInstance().mcServer.getWorlds()) {
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

    public void loadRewards() {
        Path rewardsPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/rewards.json");
        File rewardsFile = rewardsPath.toFile();

        JsonElement root;
        try {
            root = JsonParser.parseReader(new FileReader(rewardsFile));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        JsonObject rewardsObj = root.getAsJsonObject();
        CompoundRaids.LOGGER.info("[RAIDS] Loading the rewards...");

        for (String rewardPoolKey : rewardsObj.keySet()) {
            JsonObject rewardPool = rewardsObj.get(rewardPoolKey).getAsJsonObject();
            JsonObject rewards = rewardPool.getAsJsonObject("rewards");
            ArrayList<Reward> rewardList = new ArrayList<>();
            CompoundRaids.LOGGER.info("[RAIDS] Creating reward pool: {}", rewardPoolKey);
            for (String reward : rewards.keySet()) {
                JsonObject rewardSection = rewards.getAsJsonObject(reward);

                String item = rewardSection.get("item").getAsString();
                int count = rewardSection.get("count").getAsInt();
                String nbt = rewardSection.get("nbt").getAsString();

                ArrayList<String> commands = new ArrayList<>();
                for (JsonElement commandElement : rewardSection.get("commands").getAsJsonArray()) {
                    String command = commandElement.getAsString();
                    commands.add(command);
                }

                double weight = rewardSection.get("weight").getAsDouble();

                Reward rewardObject = new Reward(item, count, nbt, commands, weight);
                rewardList.add(rewardObject);

                CompoundRaids.LOGGER.info("[RAIDS] Added reward: {}", reward);
            }
            CompoundRaids.LOGGER.info("[RAIDS] Total rewards in this pool: {}", rewardList.size());

            double poolWeight = rewardPool.get("weight").getAsDouble();
            ArrayList<String> categories = new ArrayList<>();
            for (JsonElement categoryElement : rewardPool.getAsJsonArray("categories")) {
                String category = categoryElement.getAsString();
                categories.add(category);
            }

            RewardPool pool = new RewardPool(poolWeight, categories, rewardList);
            rewardPoolList.put(rewardPoolKey, pool);
        }
        CompoundRaids.LOGGER.info("[RAIDS] Loaded {} reward pools!", rewardPoolList.size());
    }

    public void loadMessages() {
        Path messagesPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/messages.json");
        File messagesFile = messagesPath.toFile();

        JsonElement root;
        try {
            root = JsonParser.parseReader(new FileReader(messagesFile));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        JsonObject messagesRoot = root.getAsJsonObject();
        String prefix = messagesRoot.get("prefix").getAsString();

        JsonObject messages = messagesRoot.getAsJsonObject("messages");
        Map<String, String> messagesMap = new HashMap<>();
        for (String message : messages.keySet()) {
            messagesMap.put(message, messages.get(message).getAsString());
        }

        messagesObject = new Messages(prefix, messagesMap);
    }

    public void loadBossbars() {
        Path messagesPath = FabricLoader.getInstance().getConfigDir().resolve("CompoundRaids/messages.json");
        File messagesFile = messagesPath.toFile();

        JsonElement root;
        try {
            root = JsonParser.parseReader(new FileReader(messagesFile));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        JsonObject messagesRoot = root.getAsJsonObject();
        JsonObject bossbars = messagesRoot.getAsJsonObject("bossbars");
        for (String bossbar : bossbars.keySet()) {
            JsonObject bossbarObj = bossbars.getAsJsonObject(bossbar);

            String phase = bossbarObj.get("phase").getAsString();

            boolean useOverlay = bossbarObj.get("useOverlay").getAsBoolean();
            String overlayText = "";
            if (useOverlay) {
                overlayText = bossbarObj.get("overlayText").getAsString();
            }

            String color = bossbarObj.get("color").getAsString();
            String style = bossbarObj.get("style").getAsString();
            String text = bossbarObj.get("text").getAsString();

            ArrayList<String> bosses = new ArrayList<>();
            for (JsonElement boss : bossbarObj.getAsJsonArray("bosses")) {
                bosses.add(boss.getAsString());
            }

            BossBar.Color bossBarColor = BossBar.Color.WHITE;

            switch (color) {
                case "blue":
                    bossBarColor = BossBar.Color.BLUE;
                    break;
                case "pink":
                    bossBarColor = BossBar.Color.PINK;
                    break;
                case "red":
                    bossBarColor = BossBar.Color.RED;
                    break;
                case "green":
                    bossBarColor = BossBar.Color.GREEN;
                    break;
                case "yellow":
                    bossBarColor = BossBar.Color.YELLOW;
                    break;
                case "purple":
                    bossBarColor = BossBar.Color.PURPLE;
                    break;
                default:
                    break;
            }

            BossBar.Overlay bossBarOverlay = BossBar.Overlay.PROGRESS;
            switch (style) {
                case "notched_6":
                    bossBarOverlay = BossBar.Overlay.NOTCHED_6;
                    break;
                case "notched_10":
                    bossBarOverlay = BossBar.Overlay.NOTCHED_10;
                    break;
                case "notched_12":
                    bossBarOverlay = BossBar.Overlay.NOTCHED_12;
                    break;
                case "notched_20":
                    bossBarOverlay = BossBar.Overlay.NOTCHED_20;
                    break;
                default:
                    break;
            }

            //BossBar bar = BossBar.bossBar(CompoundRaids.getInstance().mm.deserialize(text), 1f, bossBarColor, bossBarOverlay);
            Bossbar theBar = new Bossbar(phase, text, bossBarColor, bossBarOverlay, useOverlay, overlayText, bosses);
            bossbarList.put(bossbar, theBar);
        }
    }

    public RaidSettings getRaidSettings() {
        return raidSettings;
    }

    public Map<String, Boss> getBossList() {
        return bossList;
    }

    public Map<String, Location> getLocationList() {
        return locationList;
    }

    public Map<String, RewardPool> getRewardPoolList() {
        return rewardPoolList;
    }

    public Map<String, Category> getCategoryList() {
        return categoryList;
    }

    public Messages getMessagesObject() {
        return messagesObject;
    }

    public Map<String, Bossbar> getBossbarList() {
        return bossbarList;
    }

//    public Item getRaidVoucherItem() {
//        return raidVoucherItem;
//    }
//
//    public Item getRaidPassItem() {
//        return raidPassItem;
//    }
//
//    private Item getItem(String namespace) {
//        return CompoundRaids.getInstance().mcServer.getWorlds().iterator().next().getRegistryManager().get(RegistryKeys.ITEM).get(Identifier.of(namespace));
//    }
}
