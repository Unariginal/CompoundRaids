package me.unariginal.compoundraids.datatypes;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.unariginal.compoundraids.CompoundRaids;
import me.unariginal.compoundraids.managers.Bossbar;
import me.unariginal.compoundraids.managers.Messages;
import net.kyori.adventure.bossbar.BossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

public class Raid {
    private final CompoundRaids cr = CompoundRaids.getInstance();

    private final UUID uuid;
    private final Boss boss;
    private final PokemonEntity bossEntity;
    private final ServerWorld world;

    private int stage = 0;
    private long startTime = 0;
    private long endTime = 0;
    private long phaseStart = 0;
    private long phaseTime = 0;

    private final Map<Long, List<Task>> tasks = new HashMap<>();
    private BossBarData bossBarData;
    private BossBar bar;

    public Raid(UUID uuid, Boss boss, PokemonEntity bossEntity, ServerWorld world) {
        this.uuid = uuid;
        this.boss = boss;
        this.bossEntity = bossEntity;
        this.world = world;
    }

    public void stopRaid() {
        stage = -1;
        if (bossEntity.getUuid() == uuid) {
            if (bossEntity.isLiving()) {
                if (getBoss().bossPokemon().getLevel() > 100) {
                    try {
                        Field pokeField = getBoss().bossPokemon().getClass().getDeclaredField("level");
                        pokeField.setAccessible(true);
                        pokeField.set(getBoss().bossPokemon(), 100);
                    } catch (IllegalAccessException | NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (bossEntity.isBattling()) {
                    if (bossEntity.getBattleId() != null) {
                        if (Cobblemon.INSTANCE.getBattleRegistry().getBattle(bossEntity.getBattleId()) != null) {
                            Cobblemon.INSTANCE.getBattleRegistry().getBattle(bossEntity.getBattleId()).end();
                        }
                    }
                }
                bossEntity.kill();
            }
        }
        removeBossBar();
        CompoundRaids.LOGGER.info("[RAIDS] Raid {} ({}) stopped!", boss.bossName(), uuid);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Boss getBoss() {
        return boss;
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

    public void beginPrePhase() {
        stage = 1;
        phaseTime = cr.config.getRaidSettings().raid_prePhaseTimeSeconds();
        phaseStart = world.getTime();

        cr.config.getBossbarList().keySet().forEach(key -> {
            Bossbar barInfo = cr.config.getBossbarList().get(key);
            if (barInfo.phase().equalsIgnoreCase("prepare")) {
                if (barInfo.bosses().contains(boss.bossName())) {
                    bossBarData = new BossBarData(barInfo, phaseStart, phaseStart + (phaseTime * 20L));
                }
            }
        });

        displayBossbar();

        sendAllPlayers("text_startPrePhase");

        addTask(world, cr.config.getRaidSettings().raid_prePhaseTimeSeconds() * 20L, this::beginFightPhase);
    }

    public void beginFightPhase() {
        stage = 2;
        removeBossBar();

        phaseTime = cr.config.getRaidSettings().raid_fightPhaseTimeSeconds();
        phaseStart = world.getTime();

        cr.config.getBossbarList().keySet().forEach(key -> {
            Bossbar barInfo = cr.config.getBossbarList().get(key);
            if (barInfo.phase().equalsIgnoreCase("fight")) {
                if (barInfo.bosses().contains(boss.bossName())) {
                    bossBarData = new BossBarData(barInfo, phaseStart, phaseStart + (phaseTime * 20L));
                }
            }
        });

        displayBossbar();

        sendAllPlayers("text_startFightPhase");

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

        addTask(world, cr.config.getRaidSettings().raid_fightPhaseTimeSeconds()*20L, this::endFightPhase);
    }

    public void handleBossDefeat() {
        stage = 3;
        removeBossBar();
        endTime = Instant.now().toEpochMilli();

        phaseTime = cr.config.getRaidSettings().raid_catchWarningTimeSeconds();
        phaseStart = world.getTime();

        cr.config.getBossbarList().keySet().forEach(key -> {
            Bossbar barInfo = cr.config.getBossbarList().get(key);
            if (barInfo.phase().equalsIgnoreCase("catch-warning")) {
                if (barInfo.bosses().contains(boss.bossName())) {
                    bossBarData = new BossBarData(barInfo, phaseStart, phaseStart + (phaseTime * 20L));
                }
            }
        });

        displayBossbar();

        tasks.clear();
        addTask(world, cr.config.getRaidSettings().raid_catchWarningTimeSeconds()*20L, this::beginCatchPhase);

        sendAllPlayers("text_bossDefeated");
        sendAllPlayers("text_catchEncounterWarning");
        // leaderboard
        // rewards
    }

    public void beginCatchPhase() {
        stage = 4;
        removeBossBar();

        phaseTime = cr.config.getRaidSettings().raid_catchPhaseTimeSeconds();
        phaseStart = world.getTime();

        cr.config.getBossbarList().keySet().forEach(key -> {
            Bossbar barInfo = cr.config.getBossbarList().get(key);
            if (barInfo.phase().equalsIgnoreCase("catch")) {
                if (barInfo.bosses().contains(boss.bossName())) {
                    bossBarData = new BossBarData(barInfo, phaseStart, phaseStart + (phaseTime * 20L));
                }
            }
        });

        displayBossbar();

        addTask(world, cr.config.getRaidSettings().raid_catchPhaseTimeSeconds()*20L, this::endCatchPhase);
        sendAllPlayers("text_startCatchPhase");
    }

    public void endFightPhase() {
        stage = -1;
        sendAllPlayers("text_timesUp");
        removeBossBar();
        tasks.clear();
    }

    public void endCatchPhase() {
        stage = -1;
        sendAllPlayers("text_catchPhaseEnd");
        removeBossBar();
        tasks.clear();
    }

    private void addTask(ServerWorld world, long delay, Runnable action) {
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
        cr.mcServer.getPlayerManager().getPlayerList().forEach(player -> {
            player.hideBossBar(bar);
        });
    }

    private void displayBossbar() {
        cr.mcServer.getPlayerManager().getPlayerList().forEach(player -> {
            bar = BossBar.bossBar(cr.mm.deserialize(handlePlaceholders(bossBarData.bossbar().text())), 1f, bossBarData.bossbar().color(), bossBarData.bossbar().style());
            player.showBossBar(bar);
        });
    }

    private void sendAllPlayers(String key) {
        cr.mcServer.getPlayerManager().getPlayerList().forEach(player -> {
            player.sendMessage(cr.mm.deserialize(handlePlaceholders(cr.config.getMessagesObject().getRawMessage(key))));
        });
    }

    public BossBar getBar() {
        return bar;
    }

    public void displayOverlay(ServerPlayerEntity player) {
        if (bossBarData.bossbar().useOverlay()) {
            player.sendActionBar(cr.mm.deserialize(handlePlaceholders(bossBarData.bossbar().overlayText())));
        }
    }

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
