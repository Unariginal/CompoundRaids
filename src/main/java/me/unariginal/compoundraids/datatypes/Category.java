package me.unariginal.compoundraids.datatypes;

import java.util.ArrayList;

public record Category(boolean isRandom, int minWaitSeconds, int maxWaitSeconds, ArrayList<String> raidTimes,
                       int minPlayers, boolean voucherEnabled) {
}
