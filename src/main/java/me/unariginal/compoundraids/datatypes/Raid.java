package me.unariginal.compoundraids.datatypes;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.BattleBuilder;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.properties.UncatchableProperty;
import me.unariginal.compoundraids.CompoundRaids;
import me.unariginal.compoundraids.managers.Bossbar;
import me.unariginal.compoundraids.managers.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

public class Raid {
    private final CompoundRaids cr = CompoundRaids.getInstance();

    private final UUID uuid;
    private final Boss boss;
    private final PokemonEntity bossEntity;
    private final Vec3d position;
    private final ServerWorld world;

    private int stage = 0;
    private long startTime = 0;
    private long endTime = 0;
    private long phaseStart = 0;
    private long phaseTime = 0;

    private final Map<Long, List<Task>> tasks = new HashMap<>();
    private BossBarData bossBarData;
    private Map<ServerPlayerEntity, BossBar> bossbars = new HashMap<>();

    public ArrayList<ServerPlayerEntity> playersToBeBattled = new ArrayList<>();
    public ArrayList<ServerPlayerEntity> playersBattling = new ArrayList<>();
    public ArrayList<PokemonEntity> clones = new ArrayList<>();

    public Raid(UUID uuid, Boss boss, PokemonEntity bossEntity, Vec3d position, ServerWorld world) {
        this.uuid = uuid;
        this.boss = boss;
        this.bossEntity = bossEntity;
        this.position = position;
        this.world = world;
    }

    // Handle stopping the raid
    public void stopRaid() {
        stage = -1;
        if (bossEntity.getUuid() == uuid) {
            if (bossEntity.isLiving()) {
                // Stop battles that the boss is in
                if (bossEntity.isBattling()) {
                    if (bossEntity.getBattleId() != null) {
                        if (Cobblemon.INSTANCE.getBattleRegistry().getBattle(bossEntity.getBattleId()) != null) {
                            Cobblemon.INSTANCE.getBattleRegistry().getBattle(bossEntity.getBattleId()).end();
                        }
                    }
                }
                // Kill the boss entity
                bossEntity.kill();
            }
        }

        for (PokemonEntity entity : clones) {
            entity.kill();
        }
        // Remove the boss bar
        removeBossBar();
        CompoundRaids.LOGGER.info("[RAIDS] Raid {} ({}) stopped!", boss.bossName(), uuid);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Boss getBoss() {
        return boss;
    }

    public Vec3d getPosition() {
        return position;
    }

    public int getStage() {
        return stage;
    }

    public PokemonEntity getBossEntity() {
        return bossEntity;
    }

    public BossBarData getBossBarData() {
        return bossBarData;
    }

    // Raid will start here
    public void beginPrePhase() {
        stage = 1;
        phaseTime = cr.config.getRaidSettings().raid_prePhaseTimeSeconds();
        phaseStart = world.getTime();

        // Create the bossbar
        cr.config.getBossbarList().keySet().forEach(key -> {
            Bossbar barInfo = cr.config.getBossbarList().get(key);
            if (barInfo.phase().equalsIgnoreCase("prepare")) {
                if (barInfo.bosses().contains(boss.bossName())) {
                    bossBarData = new BossBarData(barInfo, phaseStart, phaseStart + (phaseTime * 20L));
                }
            }
        });

        // Show the bossbar
        displayBossbar();

        // Send chat message
        sendAllPlayers("text_startPrePhase");

        // Progress to next phase after x number of ticks
        addTask(world, cr.config.getRaidSettings().raid_prePhaseTimeSeconds() * 20L, this::beginFightPhase);
    }

    // Second phase, the fighting phase
    public void beginFightPhase() {
        stage = 2;
        // Remove the last boss bar
        removeBossBar();

        phaseTime = cr.config.getRaidSettings().raid_fightPhaseTimeSeconds();
        phaseStart = world.getTime();

        // Create the fight phase boss bar
        cr.config.getBossbarList().keySet().forEach(key -> {
            Bossbar barInfo = cr.config.getBossbarList().get(key);
            if (barInfo.phase().equalsIgnoreCase("fight")) {
                if (barInfo.bosses().contains(boss.bossName())) {
                    bossBarData = new BossBarData(barInfo, phaseStart, phaseStart + (phaseTime * 20L));
                }
            }
        });

        // Show the bossbar
        displayBossbar();

        // Send chat message
        sendAllPlayers("text_startFightPhase");

        // Mark the time that the raid started
        startTime = Instant.now().toEpochMilli();

        Vec3d position = bossEntity.getPos();
        // Force load chunks so the entity can be spawned
        int chunkX = (int) Math.floor(position.getX() / 16);
        int chunkZ = (int) Math.floor(position.getZ() / 16);
        world.setChunkForced(chunkX, chunkZ, true);

        // Spawn the pokemon entity based on it's set position
        world.spawnEntity(bossEntity);

        // Pokemon can't move or despawn
        bossEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9999, false, false));
        bossEntity.setPersistent();

        // Stop force loading the chunk
        world.setChunkForced(chunkX, chunkZ, false);

        // Progress to the next phase after x number of ticks
        addTask(world, cr.config.getRaidSettings().raid_fightPhaseTimeSeconds()*20L, this::endFightPhase);
    }

    // The boss was defeated
    public void handleBossDefeat() {
        stage = 3;
        // Remove the bossbar
        removeBossBar();
        // Time that the raid ended
        endTime = Instant.now().toEpochMilli();

        phaseTime = cr.config.getRaidSettings().raid_catchWarningTimeSeconds();
        phaseStart = world.getTime();

        for (PokemonEntity entity : clones) {
            entity.kill();
        }
        if (bossEntity.isAlive()) {
            bossEntity.kill();
        }

        // Catch warning boss bar
        cr.config.getBossbarList().keySet().forEach(key -> {
            Bossbar barInfo = cr.config.getBossbarList().get(key);
            if (barInfo.phase().equalsIgnoreCase("catch-warning")) {
                if (barInfo.bosses().contains(boss.bossName())) {
                    bossBarData = new BossBarData(barInfo, phaseStart, phaseStart + (phaseTime * 20L));
                }
            }
        });

        // Show the bossbar
        displayBossbar();

        // Clear all previous tasks
        tasks.clear();
        // Next phase in x ticks
        addTask(world, cr.config.getRaidSettings().raid_catchWarningTimeSeconds()*20L, this::beginCatchPhase);

        // Send chat messages
        sendAllPlayers("text_bossDefeated");
        sendAllPlayers("text_catchEncounterWarning");
        // leaderboard
        // rewards
    }

    // The catching phase
    public void beginCatchPhase() {
        stage = 4;
        // Remove previous bossbar
        removeBossBar();

        phaseTime = cr.config.getRaidSettings().raid_catchPhaseTimeSeconds();
        phaseStart = world.getTime();

        // Setup catch phase boss bar
        cr.config.getBossbarList().keySet().forEach(key -> {
            Bossbar barInfo = cr.config.getBossbarList().get(key);
            if (barInfo.phase().equalsIgnoreCase("catch")) {
                if (barInfo.bosses().contains(boss.bossName())) {
                    bossBarData = new BossBarData(barInfo, phaseStart, phaseStart + (phaseTime * 20L));
                }
            }
        });

        // Show the bossbar
        displayBossbar();

        // Next phase in x ticks
        addTask(world, cr.config.getRaidSettings().raid_catchPhaseTimeSeconds()*20L, this::endCatchPhase);
        // Send chat message
        sendAllPlayers("text_startCatchPhase");

        for (ServerPlayerEntity player : cr.mcServer.getPlayerManager().getPlayerList()) { // Replace with players participating in raid
            createCatchEncounter(player);
        }
    }

    private void createCatchEncounter(ServerPlayerEntity player) {
        Vec3d position = player.getPos();
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        party.heal();

        if (party.size() <= 0) {
            player.sendMessage(Text.literal("No Pokemon in party!"));
            return;
        }

        Pokemon bossPokemon = boss.bossPokemon();
        Pokemon bossClone = new Pokemon();
        bossClone.setSpecies(bossPokemon.getSpecies());

        try {
            Field pokeField = bossClone.getClass().getDeclaredField("level");
            pokeField.setAccessible(true);
            pokeField.set(bossClone, bossPokemon.getLevel());
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }

        bossClone.getCustomProperties().clear();
        PokemonProperties.Companion.parse(bossPokemon.getForm().getName()).apply(bossClone);
        bossClone.setGender(bossPokemon.getGender());
        bossClone.updateAbility(bossPokemon.getAbility());
        bossClone.setShiny(bossPokemon.getShiny());
        bossClone.setNature(bossPokemon.getNature());
        bossClone.setEvs$common(bossPokemon.getEvs());
        bossClone.setIvs$common(bossPokemon.getIvs());
        bossClone.getMoveSet().copyFrom(bossPokemon.getMoveSet());
        bossClone.setCurrentHealth(bossPokemon.getCurrentHealth());

        bossClone.getCustomProperties().add(UncatchableProperty.INSTANCE.uncatchable());

        ServerWorld world = player.getServerWorld();

        PokemonEntity entity = new PokemonEntity(world, bossClone, CobblemonEntities.POKEMON);
        entity.setPosition(position);
        world.spawnEntity(entity);
        clones.add(entity);

        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, -1, 9999, false, false));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, -1, 9999, false, false));


        UUID leadingPokemon = null;
        for (Pokemon pokemon : party) {
            if (!pokemon.isFainted()) {
                leadingPokemon = pokemon.getUuid();
                break;
            }
        }

        UUID finalLeadingPokemon = leadingPokemon;
        addTask(world, 1, () -> BattleBuilder.INSTANCE.pve(player, entity, finalLeadingPokemon, BattleFormat.Companion.getGEN_9_SINGLES(), false, true));
    }

    // Ran out of time!
    public void endFightPhase() {
        stage = -1;
        sendAllPlayers("text_timesUp");
        removeBossBar();
        tasks.clear();
    }

    // The catch phase is over, raid is finished
    public void endCatchPhase() {
        stage = -1;
        sendAllPlayers("text_catchPhaseEnd");
        removeBossBar();
        tasks.clear();
    }

    // Handles adding tasks to the task map
    public void addTask(ServerWorld world, long delay, Runnable action) {
        long tick = world.getTime();
        long executeTick = tick + delay;
        Task task = new Task(world, executeTick, action);
        if (tasks.containsKey(executeTick)) {
            List<Task> taskList = tasks.get(executeTick);
            taskList.add(task);
            tasks.replace(executeTick, taskList);
        } else {
            List<Task> taskList = new ArrayList<>();
            taskList.add(task);
            tasks.put(executeTick, taskList);
        }
    }

    public Map<Long, List<Task>> getTasks() {
        return tasks;
    }

    private void removeBossBar() {
        for (ServerPlayerEntity player : bossbars.keySet()) {
            player.hideBossBar(bossbars.get(player));
        }
    }

    private void displayBossbar() {
        cr.mcServer.getPlayerManager().getPlayerList().forEach(player -> {
            BossBar bossbar = BossBar.bossBar(cr.mm.deserialize(handlePlaceholders(bossBarData.bossbar().text())), 1f, bossBarData.bossbar().color(), bossBarData.bossbar().style());
            player.showBossBar(bossbar);
            bossbars.put(player, bossbar);
        });
    }

    private void sendAllPlayers(String key) {
        cr.mcServer.getPlayerManager().getPlayerList().forEach(player -> player.sendMessage(cr.mm.deserialize(handlePlaceholders(cr.config.getMessagesObject().getRawMessage(key)))));
    }

    public Map<ServerPlayerEntity, BossBar> getBar() {
        return bossbars;
    }

    public void displayOverlay(ServerPlayerEntity player) {
        if (bossBarData.bossbar().useOverlay()) {
            player.sendActionBar(cr.mm.deserialize(handlePlaceholders(bossBarData.bossbar().overlayText())));
        }
    }

    // Replace placeholders in messages!
    private String handlePlaceholders(String message) {
        Messages msgs = cr.config.getMessagesObject();
        message = message.replaceAll("%prefix%", msgs.getPrefix());
        message = message.replaceAll("%boss%", boss.bossName());
        message = message.replaceAll("%phase_time%", getHMS(phaseTime));
        message = message.replaceAll("%form%", boss.bossPokemon().getForm().getName());
        message = message.replaceAll("%pokemon%", boss.bossPokemon().getSpecies().getName());

        long fightTime = 0;
        if (startTime < endTime) {
            fightTime = endTime - startTime;
            fightTime /= 1000;
        }

        message = message.replaceAll("%time%", getHMS(fightTime));

        int id = 0;
        for (id = 0; id < cr.activeRaids.size(); id++) {
            if (cr.activeRaids.get(id).getUuid() == uuid) {
                break;
            }
        }
        message = message.replaceAll("%id%", String.valueOf(id + 1));

        long timeLeft = (phaseStart + (phaseTime * 20L)) - world.getTime();
        timeLeft /= 20;
        message = message.replaceAll("%timer%", getHMS(timeLeft));

        message = message.replaceAll("%currenthp%", String.valueOf(boss.hp()));
        message = message.replaceAll("%maxhp%", String.valueOf(boss.bossPokemon().getMaxHealth()));

        return message;
    }

    private String getHMS(long rawTime) {
        String formattedTime = "";
        long hours = 0;
        long minutes = 0;
        long seconds = rawTime;
        long temp = 0;
        if (rawTime >= 3600) {
            seconds = rawTime % 3600;
            hours = (rawTime - seconds) / 3600;
            formattedTime = formattedTime.concat(hours + "h ");
        }
        temp = seconds;
        seconds = seconds % 60;
        temp = temp - seconds;
        minutes = temp / 60;
        if (minutes > 0) {
            formattedTime = formattedTime.concat(minutes + "m ");
        }
        formattedTime = formattedTime.concat(seconds + "s");

        return formattedTime;
    }
}
