package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.player.CloudPlayer;

public class XpCommand extends Command {
    public XpCommand() {
        super("xp", CommandData.builder("xp")
                .setDescription("commands.xp.description")
                .setUsageMessage("/xp <amount>[L] [player]")
                .setPermissions("cloudburst.command.xp")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("amount|level", CommandParamType.INT, false),
                        new CommandParameter("player", CommandParamType.TARGET, true)
                }).build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        //  "/xp <amount> [player]"  for adding exp
        //  "/xp <amount>L [player]" for adding exp level
        String amountString;
        String playerName;
        CloudPlayer player;
        if (!(sender instanceof CloudPlayer)) {
            if (args.length != 2) {
                return false;
            }
            amountString = args[0];
            playerName = args[1];
            player = (CloudPlayer) sender.getServer().getPlayer(playerName);
        } else {
            if (args.length == 1) {
                amountString = args[0];
                player = (CloudPlayer) sender;
            } else if (args.length == 2) {
                amountString = args[0];
                playerName = args[1];
                player = (CloudPlayer) sender.getServer().getPlayer(playerName);
            } else {
                return false;
            }
        }

        if (player == null) {
            sender.sendMessage(Component.translatable("commands.generic.player.notFound").color(NamedTextColor.RED));
            return true;
        }

        int amount;
        boolean isLevel = false;
        if (amountString.endsWith("l") || amountString.endsWith("L")) {
            amountString = amountString.substring(0, amountString.length() - 1);
            isLevel = true;
        }

        try {
            amount = Integer.parseInt(amountString);
        } catch (NumberFormatException e1) {
            return false;
        }

        if (isLevel) {
            int newLevel = player.getExperienceLevel();
            newLevel += amount;
            if (newLevel > 24791) newLevel = 24791;
            if (newLevel < 0) {
                player.setExperience(0, 0);
            } else {
                player.setExperience(player.getExperience(), newLevel);
            }
            if (amount > 0) {
                sender.sendMessage(Component.translatable("commands.xp.success.levels", Component.text(amount), Component.text(player.getName())));
            } else {
                sender.sendMessage(Component.translatable("commands.xp.success.negative.levels", Component.text(-amount), Component.text(player.getName())));
            }
            return true;
        } else {
            if (amount < 0) {
                return false;
            }
            player.addExperience(amount);
            sender.sendMessage(Component.translatable("commands.xp.success", Component.text(amount), Component.text(player.getName())));
            return true;
        }
    }
}
