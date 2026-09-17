package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentConstraint;
import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.level.gamerule.*;
import org.cloudburstmc.api.registry.GameRuleRegistry;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudGameRuleRegistry;

import java.util.*;

public class GameruleCommand extends AdvertisedCommand {
    private static final Set<CommandArgumentConstraint> CHEATS_ENABLED = Set.of(CommandArgumentConstraint.CHEATS_ENABLED);

    private final GameRuleRegistry registry;

    public GameruleCommand() {
        this(CloudGameRuleRegistry.get());
    }

    public GameruleCommand(GameRuleRegistry registry) {
        super("gamerule", "commands.gamerule.description", CommandNetworkData.NOT_CHEAT, "cloudburst.command.gamerule");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.executes(this::executeCommand);

        List<GameRule<Boolean>> booleanRules = new ArrayList<>();
        List<GameRule<Integer>> integerRules = new ArrayList<>();
        List<GameRule<Float>> floatRules = new ArrayList<>();
        List<EnumGameRule<?>> enumRules = new ArrayList<>();

        for (GameRule<?> rule : this.registry.getRules()) {
            switch (rule) {
                case BooleanGameRule booleanRule -> booleanRules.add(booleanRule);
                case IntegerGameRule integerRule -> integerRules.add(integerRule);
                case FloatGameRule floatRule -> floatRules.add(floatRule);
                case EnumGameRule<?> enumRule -> enumRules.add(enumRule);
                default -> throw new IllegalStateException("Unsupported game rule type: " + rule.getClass().getName());
            }
        }

        addRuleGroup(builder, "booleanRule", "BoolGameRule", booleanRules, CommandArgumentTypes.bool(), true, true);
        addRuleGroup(builder, "integerRule", "IntGameRule", integerRules, CommandArgumentTypes.integer(), true, true);
        addRuleGroup(builder, "floatRule", "FloatGameRule", floatRules, CommandArgumentTypes.floatingPoint(), true, true);

        for (EnumGameRule<?> rule : enumRules) {
            addRuleGroup(builder, rule.getName() + "Rule", rule.getName() + "Rule", List.of(rule), rule.argumentType(), false, false);
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
        for (String rule : this.registry.getRuleNames()) {
            rulesJoiner.add(rule.toLowerCase(Locale.ROOT));
        }

        sender.sendMessage(Component.text(rulesJoiner.toString()));
        return success();
    }

    private static <T extends Comparable<T>> void addRuleGroup(
            LiteralArgumentBuilder<CommandSourceStack> builder,
            String nodeName,
            String enumName,
            List<? extends GameRule<T>> rules,
            CommandArgumentType<T> valueArgument,
            boolean querySupported,
            boolean requiredInSyntax
    ) {
        if (rules.isEmpty()) {
            return;
        }

        Map<String, GameRule<T>> rulesByName = new LinkedHashMap<>();
        Map<String, Set<CommandArgumentConstraint>> constraintsByName = new LinkedHashMap<>();
        for (GameRule<T> rule : rules) {
            String name = rule.getName().toLowerCase(Locale.ROOT);
            rulesByName.put(name, rule);
            if (rule.requiresCheats()) {
                constraintsByName.put(name, CHEATS_ENABLED);
            }
        }

        RequiredArgumentBuilder<CommandSourceStack, String> ruleBranch = Commands.argument(nodeName, CommandArgumentTypes.fixedEnumMapped(
                "rule", enumName, requiredInSyntax,
                constraintsByName,
                value -> value,
                rulesByName.keySet().toArray(String[]::new)));
        if (querySupported) {
            ruleBranch.executes(context -> queryRule(context, selectedRule(context, nodeName, rulesByName)));
        }

        ruleBranch.then(Commands.argument("value", valueArgument)
                .executes(context -> setRule(context, selectedRule(context, nodeName, rulesByName))));
        builder.then(ruleBranch);
    }

    private static <T extends Comparable<T>> GameRule<T> selectedRule(
            CommandContext<CommandSourceStack> context,
            String nodeName,
            Map<String, GameRule<T>> rulesByName
    ) {
        String ruleName = context.getArgument(nodeName, String.class);
        return Objects.requireNonNull(rulesByName.get(ruleName), "rule");
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
