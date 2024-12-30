package me.unariginal.compoundraids.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import me.unariginal.compoundraids.CompoundRaids;
import me.unariginal.compoundraids.config.Config;
import me.unariginal.compoundraids.datatypes.Raid;
import me.unariginal.compoundraids.managers.Messages;
import me.unariginal.compoundraids.managers.StartRaid;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class RaidCommands {
    CompoundRaids cr = CompoundRaids.getInstance();
    MiniMessage mm = cr.mm;

    public RaidCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("raid")
                        .then(
                                CommandManager.literal("reload")
                                        .requires(Permissions.require("cc.raids.reload", 4))
                                        .executes(this::reload)
                        )
                        .then(
                                CommandManager.literal("start")
                                        .requires(Permissions.require("cc.raids.start", 4))
                                        .then(
                                                CommandManager.argument("boss", StringArgumentType.string())
                                                        .suggests(new BossSuggestions())
                                                        .executes(this::start)
                                        )
                        )
                        .then(
                                CommandManager.literal("stop")
                                        .requires(Permissions.require("cc.raids.stop", 4))
                                        .then(
                                                CommandManager.argument("id", IntegerArgumentType.integer())
                                                        .executes(this::stop)
                                        )
                        )
                        .then(
                                CommandManager.literal("give")
                                        .requires(Permissions.require("cc.raids.give",4))
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("item", StringArgumentType.string())
                                                        .suggests(new ItemSuggestions())
                                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                                                                .executes(this::give)
                                                        )
                                                )
                                        )
                        )
                        .then(
                                CommandManager.literal("list")
                                        .requires(Permissions.require("cc.raids.list", 4))
                                        .executes(this::list)
                        )
        ));
    }

    private int reload(CommandContext<ServerCommandSource> ctx) {
        cr.config = new Config();
        return 1;
    }

    private int start(CommandContext<ServerCommandSource> ctx) {
        return StartRaid.start(ctx);
    }

    private int stop(CommandContext<ServerCommandSource> ctx) {
        Messages messages = cr.config.getMessagesObject();
        int id = IntegerArgumentType.getInteger(ctx, "id");

        if (cr.activeRaids.contains(cr.activeRaids.get(id - 1))) {
            cr.activeRaids.get(id - 1).stopRaid();
        }

        String parsedMessage = messages.getRawMessage("text_raidStopped").replaceAll("%prefix%", messages.getPrefix()).replaceAll("%id%", String.valueOf(id));
        ctx.getSource().sendMessage(mm.deserialize(parsedMessage));

        return 1;
    }

    private int give(CommandContext<ServerCommandSource> ctx) {
        return 1;
    }

    private int list(CommandContext<ServerCommandSource> ctx) {
        Messages messages = cr.config.getMessagesObject();
        if (cr.activeRaids.isEmpty()) {
            ctx.getSource().sendMessage(mm.deserialize(messages.getRawMessage("text_noActiveRaids").replaceAll("%prefix%", messages.getPrefix())));
            return 1;
        }

        ctx.getSource().sendMessage(mm.deserialize(messages.getRawMessage("text_listRaidsHeader")));
        for (int id = 0; id < cr.activeRaids.size(); id++) {
            Raid thisRaid = cr.activeRaids.get(id);
            String parsedMessage = messages.getRawMessage("text_listRaidsBody")
                    .replaceAll("%id%", String.valueOf(id + 1))
                    .replaceAll("%boss%", thisRaid.getBoss().bossName())
                    .replaceAll("%uuid%", thisRaid.getUuid().toString())
                    .replaceAll("%species%", thisRaid.getBoss().bossPokemon().getSpecies().toString());
            ctx.getSource().sendMessage(mm.deserialize(parsedMessage));
        }

        return 1;
    }
}
