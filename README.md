# CompoundRaids! A raid event handler for Cobblemon v1.6

**Planned Features**

-- Commands --
- /raid reload (used to reload the configurations)
- /raid start <boss/random/category> [<location/random>] (used to manually start a raid for a specific boss, or a random boss, or a random boss from a category of bosses. Ability to specify any location from the locations.json config rather than the locations set in the boss config, or a random location)
- /raid stop (stops all active raids, until I add a feature to specify which raid to stop.)
- /raid give <player> <item> <amount> (give player an item from this mod. Items will include: Raid Vouchers for specific bosses, specific categories of bosses, or random. Raid Passes, again for specific bosses, categories, or any raid, used to be granted access to participate in locked raids)

-- Bosses --

You've probably gained a bit of insight as to how bosses will be handled from the sections above this, but here's how they'll work more specifically.
There will be a single bosses.json file which each possible raid boss will be located.
Each boss section will contain information about the pokemon to spawn, such as species, level, ivs, evs, moves, etc..
The section will also include which locations you'd like the boss to be able to spawn at when a raid is started naturally. These locations also have a weight to them, so you can favor some locations over others.
The boss will also have a category option on it, this is intended to be used to categorize bosses by rarity, for the sake of reward distribution.

-- Locations --

A locations.json file will also exist, here you can specify the name of each location, and include the coordinates and world that you'd like the boss to spawn at if it uses that location.

-- Categories --

A categories.json file will exist, here you can specify whatever category of raid you'd like to. By default, a common, uncommon, rare, and legendary section will exist.
Within these sections, you can specify if you'd like raids from this category to randomly occur, given a minimum number of players online.
Or, if you disable the random option, it can go based on set raid times, such as "9:45", "12:00", "16:00", etc.
Both options can also be enabled at once, for both random raids and set raids to occur at once.
If both options are disabled/left blank, it will be considered a voucher only raid.
If vouchers are also disabled for the category, it will be a start-by-command-only raid.

-- Rewards --

A rewards.json file will exist, this will be categorized by reward pools.
A reward pool can contain one or multiple individual rewards, and each reward pool will have a categories section so you can specify which categories of raid you'd like the pool to apply to.
Each reward pool will also have a weight to it, so when determining which reward to give to players based on the category of raid, it can be fully or semi random.
Each reward within the reward pool can have an item, item count, item NBT data, and a command to execute within it.
Each reward also has a weight to it, if you'd like to only give some items from a reward pool.
Reward pools will also have an option to set how many times it will pull from the pool, and if you'd like to exclude duplicate rewards.

-- Messages --

A messages.json file will also exist, this is fairly self explanitory.
It will contain every message I can think to add to it, with placeholders, etc.
It will also contain all bossbar set ups.
