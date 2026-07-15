package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.level.gamerule.*;
import org.cloudburstmc.api.registry.GameRuleRegistry;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamOption;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

public class GameruleCommand extends Command {
    private static final GameRuleRegistry registry = CloudServer.getInstance().getGameRuleRegistry();

    public GameruleCommand() {
        super("gamerule", CommandData.builder("gamerule")
                .setDescription("commands.gamerule.description")
                .setUsageMessage("/gamerule <gamerule> [value]")
                .setPermissions("cloudburst.command.gamerule")
                .setParameters(createParameters())
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (!(sender instanceof CloudPlayer)) {
            sender.sendMessage(Component.translatable("commands.locate.fail.noplayer"));
            return true;
        }
        GameRuleMap rules = ((CloudPlayer) sender).getLevel().getGameRules();

        switch (args.length) {
            case 0:
                StringJoiner rulesJoiner = new StringJoiner(", ");
                for (String rule : registry.getRuleNames()) {
                    rulesJoiner.add(rule.toLowerCase());
                }
                sender.sendMessage(Component.text(rulesJoiner.toString()));
                return true;
            case 1:
                GameRule<?> gameRule = registry.fromString(args[0]);
                if (gameRule == null || !rules.contains(gameRule)) {
                    sender.sendMessage(Component.translatable("commands.generic.syntax",
                            Component.text("/gamerule"), Component.text(args[0])));
                    return true;
                }

                sender.sendMessage(Component.text(gameRule.getName() + " = " + formatValue(gameRule, rules.getValue(gameRule))));
                return true;
            default:
                gameRule = registry.fromString(args[0]);

                if (gameRule == null) {
                    sender.sendMessage(Component.translatable("commands.generic.syntax",
                            Component.text("/gamerule "), Component.text(args[0]),
                            Component.text(" " + String.join(" ", Arrays.copyOfRange(args, 1, args.length)))));
                    return true;
                }

                try {
                    rules.parseAndSet(gameRule, args[1]);
                    sender.sendMessage(Component.translatable("commands.gamerule.success", Component.text(gameRule.getName()), Component.text(args[1])));
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.translatable("commands.generic.syntax",
                            Component.text("/gamerule " + args[0] + " "), Component.text(args[1]),
                            Component.text(" " + String.join(" ", Arrays.copyOfRange(args, 2, args.length)))));
                }
                return true;
        }
    }

    private static String formatValue(GameRule<?> gameRule, Object value) {
        if (gameRule instanceof EnumGameRule<?> enumGameRule) {
            return enumGameRule.getSerializedValue((int) value);
        }
        return value.toString();
    }

    private static List<CommandParameter[]> createParameters() {
        List<CommandParameter[]> parameters = new ArrayList<>();
        List<String> booleanRules = new ArrayList<>();
        List<String> integerRules = new ArrayList<>();

        parameters.add(new CommandParameter[]{});
        for (GameRule<?> rule : registry.getRules()) {
            if (rule instanceof EnumGameRule<?> enumGameRule) {
                parameters.add(new CommandParameter[]{
                        new CommandParameter("rule", false, enumGameRule.getName() + "Rule", new String[]{enumGameRule.getName()}),
                        new CommandParameter("value", false, enumGameRule.getName() + "Values", enumGameRule.getValues().toArray(new String[0]))
                });
            } else if (rule instanceof BooleanGameRule) {
                booleanRules.add(rule.getName());
            } else if (rule instanceof IntegerGameRule) {
                integerRules.add(rule.getName());
            }
        }

        if (!booleanRules.isEmpty()) {
            parameters.add(new CommandParameter[]{
                    new CommandParameter("rule", false, "BoolGameRule", booleanRules.toArray(new String[0]), CommandParamOption.HAS_SEMANTIC_CONSTRAINT),
                    new CommandParameter("value", true, "Boolean", new String[]{"true", "false"})
            });
        }

        if (!integerRules.isEmpty()) {
            parameters.add(new CommandParameter[]{
                    new CommandParameter("rule", false, "IntGameRule", integerRules.toArray(new String[0]), CommandParamOption.HAS_SEMANTIC_CONSTRAINT),
                    new CommandParameter("value", CommandParamType.INT, true)
            });
        }

        return parameters;
    }
}
