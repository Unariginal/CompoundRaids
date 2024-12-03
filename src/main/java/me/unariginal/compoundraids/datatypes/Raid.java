package me.unariginal.compoundraids.datatypes;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import me.unariginal.compoundraids.CompoundRaids;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.time.Instant;
import java.util.*;

public class Raid {
    private CompoundRaids cr = CompoundRaids.getInstance();

    private final UUID uuid;
    private final Boss boss;
    private final PokemonEntity bossEntity;
    private final ServerWorld world;

    private int stage = 0;
    private long startTime = 0;

    public Map<Long, List<Task>> tasks = new HashMap<>();

    public Raid(UUID uuid, Boss boss, PokemonEntity bossEntity, ServerWorld world) {
        this.uuid = uuid;
        this.boss = boss;
        this.bossEntity = bossEntity;
        this.world = world;
    }

    public void stopRaid() {
        stage = -1;
        bossEntity.kill();
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

    public void beginPrePhase() {
        stage = 1;
        //pre boss bar
        //pre phase message
        CompoundRaids.LOGGER.info("[Raids] Pre Phase Started");
        CompoundRaids.LOGGER.info("[Raids] Next Phase In {} Seconds..", cr.config.getRaidSettings().raid_prePhaseTimeSeconds());
        addTask(world, cr.config.getRaidSettings().raid_prePhaseTimeSeconds() * 20L, this::beginFightPhase);
    }

    public void beginFightPhase() {
        stage = 2;
        //disable old boss bar
        //fight boss bar
        //fight phase message
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

        CompoundRaids.LOGGER.info("[RAIDS] {} was spawned", bossEntity.getPokemon().getSpecies());
        CompoundRaids.LOGGER.info("[RAIDS] Fight Phase Started");
        addTask(world, cr.config.getRaidSettings().raid_fightPhaseTimeSeconds()*20L, this::endFightPhase);
    }

    public void handleBossDefeat() {
        stage = 3;
        // disable boss bars
        // catch warning boss bar
        tasks.clear();
        addTask(world, cr.config.getRaidSettings().raid_catchWarningTimeSeconds()*20L, this::beginCatchPhase);
        // messages
        // leaderboard
        // rewards
        CompoundRaids.LOGGER.info("[Raids] Boss Defeated");
    }

    public void beginCatchPhase() {
        // disable boss bars
        // catch phase boss bar
        stage = 4;
        addTask(world, cr.config.getRaidSettings().raid_catchPhaseTimeSeconds()*20L, this::endCatchPhase);
        // catch message
        CompoundRaids.LOGGER.info("[Raids] Begin Catch Phase");
    }

    public void endFightPhase() {
        stage = -1;
        // LOSER
        CompoundRaids.LOGGER.info("[Raids] Times Up");
    }

    public void endCatchPhase() {
        stage = -1;
        CompoundRaids.LOGGER.info("[Raids] Catch Phase Over");
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
}
