package me.unariginal.compoundraids.managers;

import net.kyori.adventure.bossbar.BossBar;

import java.util.ArrayList;

public record Bossbar(String phase, String text, BossBar.Color color, BossBar.Overlay style, boolean useOverlay, String overlayText, ArrayList<String> bosses) {
}
