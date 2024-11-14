package me.unariginal.compoundraids.datatypes;

import java.util.ArrayList;

public record RewardPool(double poolWeight, ArrayList<String> categories, ArrayList<Reward> rewards) {
}
