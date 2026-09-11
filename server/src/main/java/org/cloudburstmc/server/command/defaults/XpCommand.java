package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;
import java.util.OptionalInt;

public class XpCommand extends AdvertisedCommand {
    public XpCommand() {
        super("xp", "commands.xp.description", CommandNetworkData.GAME_DIRECTORS, "cloudburst.command.xp");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("amount", CommandArgumentTypes.integer(0))
                .executes(this::executeCommand)
                .then(Commands.argument("player", arguments.players())
                        .executes(this::executeCommand)));
        builder.then(Commands.argument("levelAmount", CommandArgumentTypes.postfix("amount", "l"))
                .executes(this::executeCommand)
                .then(Commands.argument("levelPlayer", arguments.players("player"))
                        .executes(this::executeCommand)));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        int amount;
        boolean isLevel;
        List<CloudPlayer> selectedPlayers = null;
        if (hasArgument(context, "amount")) {
            amount = argumentValue(context, "amount", Integer.class);
            isLevel = false;
            if (hasArgument(context, "player")) {
                selectedPlayers = cloudPlayersArgument(context, "player");
            }
        } else {
            String amountString = argumentValue(context, "levelAmount");
            OptionalInt levelAmount = parsePostfixedInteger(amountString, "l");
            isLevel = true;

            if (hasArgument(context, "levelPlayer")) {
                selectedPlayers = cloudPlayersArgument(context, "levelPlayer");
            }

            if (levelAmount.isEmpty()) {
                return usage();
            }

            amount = levelAmount.getAsInt();
        }

        List<CloudPlayer> players;
        if (selectedPlayers != null) {
            if (selectedPlayers.isEmpty()) {
                return success();
            }
            players = selectedPlayers;
        } else if (sender instanceof CloudPlayer cloudPlayer) {
            players = List.of(cloudPlayer);
        } else {
            return usage();
        }

        for (CloudPlayer player : players) {
            if (isLevel) {
                long requestedLevel = (long) player.getExperienceLevel() + amount;
                int newLevel = (int) Math.min(requestedLevel, 24791L);

                if (requestedLevel < 0) {
                    player.setExperience(0, 0);
                } else {
                    player.setExperience(player.getExperience(), newLevel);
                }

                if (amount > 0) {
                    sender.sendMessage(Component.translatable("commands.xp.success.levels",
                            Component.text(amount), Component.text(player.getName())));
                } else {
                    sender.sendMessage(Component.translatable("commands.xp.success.negative.levels",
                            Component.text(-amount), Component.text(player.getName())));
                }
            } else {
                player.addExperience(amount);
                sender.sendMessage(Component.translatable("commands.xp.success",
                        Component.text(amount), Component.text(player.getName())));
            }
        }
        return success();
    }

    private static OptionalInt parsePostfixedInteger(String value, String postfix) {
        if (value.length() < postfix.length()
                || !value.regionMatches(true, value.length() - postfix.length(), postfix, 0, postfix.length())) {
            return OptionalInt.empty();
        }

        try {
            return OptionalInt.of(Integer.parseInt(value.substring(0, value.length() - postfix.length())));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
