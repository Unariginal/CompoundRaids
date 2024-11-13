package me.unariginal.compoundraids.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.unariginal.compoundraids.CompoundRaids;
import me.unariginal.compoundraids.datatypes.Boss;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class BossSuggestions implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> commandContext, SuggestionsBuilder suggestionsBuilder) throws CommandSyntaxException {
        for (Boss boss : CompoundRaids.getInstance().config.getBossList().values().stream().toList()) {
            if (boss.bossName() != null && CommandSource.shouldSuggest(suggestionsBuilder.getRemaining(), boss.bossName())) {
                suggestionsBuilder.suggest(boss.bossName());
            }
        }

        return suggestionsBuilder.buildFuture();
    }
}
