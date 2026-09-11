package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.level.gamerule.GameRule;
import org.cloudburstmc.api.level.gamerule.LevelGameRules;
import org.cloudburstmc.api.registry.GameRuleRegistry;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Locale;
import java.util.StringJoiner;

public class GameruleCommand extends AdvertisedCommand {
    private static final GameRuleRegistry REGISTRY = CloudServer.getInstance().getGameRuleRegistry();

    public GameruleCommand() {
        super("gamerule", "commands.gamerule.description", CommandNetworkData.GAME_DIRECTORS_NOT_CHEAT,
                "cloudburst.command.gamerule");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.executes(this::executeCommand);

        for (GameRule<?> rule : REGISTRY.getRules()) {
            builder.then(ruleBranch(rule));
        }
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        if (!(sender instanceof CloudPlayer)) {
            sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
            return success();
        }

        StringJoiner rulesJoiner = new StringJoiner(", ");
        for (String rule : REGISTRY.getRuleNames()) {
            rulesJoiner.add(rule.toLowerCase(Locale.ROOT));
        }
        sender.sendMessage(Component.text(rulesJoiner.toString()));
        return success();
    }

    private static <T extends Comparable<T>> LiteralArgumentBuilder<CommandSourceStack> ruleBranch(GameRule<T> gameRule) {
        return Commands.literal(gameRule.getName())
                .executes(context -> queryRule(context, gameRule))
                .then(Commands.argument("value", gameRule.argumentType())
                        .executes(context -> setRule(context, gameRule)));
    }

    private static <T extends Comparable<T>> int queryRule(CommandContext<CommandSourceStack> context, GameRule<T> gameRule) {
        CommandSender sender = sender(context);
        LevelGameRules rules = playerRules(sender);
        if (rules == null || !rules.contains(gameRule)) {
            return success();
        }
        T value = rules.get(gameRule);
        sender.sendMessage(Component.text(gameRule.getName() + " = " + gameRule.serialize(value)));
        return success();
    }

    private static <T extends Comparable<T>> int setRule(CommandContext<CommandSourceStack> context, GameRule<T> gameRule) {
        CommandSender sender = sender(context);
        LevelGameRules rules = playerRules(sender);
        if (rules == null || !rules.contains(gameRule)) {
            return success();
        }
        T value = context.getArgument("value", gameRule.getValueClass());
        rules.set(gameRule, value);
        sender.sendMessage(Component.translatable("commands.gamerule.success",
                Component.text(gameRule.getName()), Component.text(gameRule.serialize(value))));
        return success();
    }

    private static LevelGameRules playerRules(CommandSender sender) {
        if (sender instanceof CloudPlayer player) {
            return player.getLevel().getGameRules();
        }
        sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
        return null;
    }
}
