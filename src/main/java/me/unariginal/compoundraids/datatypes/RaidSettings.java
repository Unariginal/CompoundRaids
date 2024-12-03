package me.unariginal.compoundraids.datatypes;

public record RaidSettings(String timezone, long raid_prePhaseTimeSeconds, long raid_fightPhaseTimeSeconds, long raid_afterFightCooldownSeconds, long raid_catchPhaseTimeSeconds, long raid_catchWarningTimeSeconds, long raid_healthIncreasePerPlayer) {
}
