package me.unariginal.compoundraids.datatypes;

import me.unariginal.compoundraids.managers.Bossbar;

public record BossBarData(Bossbar bossbar, long startTick, long endTick) {
}
