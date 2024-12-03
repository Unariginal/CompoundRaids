package me.unariginal.compoundraids.datatypes;

import net.minecraft.server.world.ServerWorld;

public record Task(ServerWorld world, Long tick, Runnable action) {

}
