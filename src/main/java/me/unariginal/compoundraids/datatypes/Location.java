package me.unariginal.compoundraids.datatypes;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public record Location(Vec3d coordinates, ServerWorld world) {
}
