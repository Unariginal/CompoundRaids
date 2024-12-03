package me.unariginal.compoundraids.datatypes;

import net.kyori.adventure.bossbar.BossBar;

public record BossBarData(BossBar bossBar, long startTick, long endTick) {
}
