package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.player.CloudPlayer;

public class TitleCommand extends Command {
    public TitleCommand() {
        super("title", CommandData.builder("title")
                .setDescription("commands.title.description")
                .setUsageMessage("/title <player> <clear|reset>\n/title <player> <title|subtitle|actionbar> <text>\n/title <player> <times> <fadein> <stay> <fadeOut>")
                .setPermissions("cloudburst.command.title")
                .addParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("clear", new String[]{"clear"})})
                .addParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("reset", new String[]{"reset"})})
                .addParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("title", new String[]{"title"}),
                        new CommandParameter("titleText", CommandParamType.STRING, false)})
                .addParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("subtitle", new String[]{"subtitle"}),
                        new CommandParameter("titleText", CommandParamType.STRING, false)})
                .addParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("actionbar", new String[]{"actionbar"}),
                        new CommandParameter("titleText", CommandParamType.STRING, false)})
                .addParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("times", new String[]{"times"}),
                        new CommandParameter("fadeIn", CommandParamType.INT, false),
                        new CommandParameter("stay", CommandParamType.INT, false),
                        new CommandParameter("fadeOut", CommandParamType.INT, false)})
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length < 2) {
            return false;
        }

        CloudPlayer player = CloudServer.getInstance().getPlayerExact(args[0]);
        if (player == null) {
            sender.sendMessage(Component.translatable("commands.generic.player.notFound").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 2) {
            switch (args[1].toLowerCase()) {
                case "clear":
                    player.clearTitle();
                    sender.sendMessage(Component.translatable("cloudburst.command.title.clear", Component.text(player.getName())));
                    break;
                case "reset":
                    player.resetTitle();
                    sender.sendMessage(Component.translatable("cloudburst.command.title.reset", Component.text(player.getName())));
                    break;
                default:
                    return false;
            }
        } else if (args.length == 3) {
            switch (args[1].toLowerCase()) {
                case "title":
                    player.sendTitle(Component.text(args[2]));
                    sender.sendMessage(Component.translatable("cloudburst.command.title.title", Component.text(args[2]), Component.text(player.getName())));
                    break;
                case "subtitle":
                    player.sendSubtitle(Component.text(args[2]));
                    sender.sendMessage(Component.translatable("cloudburst.command.title.subtitle", Component.text(args[2]), Component.text(player.getName())));
                    break;
                case "actionbar":
                    player.sendActionBar(Component.text(args[2]));
                    sender.sendMessage(Component.translatable("cloudburst.command.title.actionbar", Component.text(args[2]), Component.text(player.getName())));
                    break;
                default:
                    return false;
            }
        } else if (args.length == 5) {
            if (args[1].equalsIgnoreCase("times")) {
                try {
                    int fadeIn = Integer.parseInt(args[2]);
                    int stay = Integer.parseInt(args[3]);
                    int fadeOut = Integer.parseInt(args[4]);
                    player.setTitleTimes(fadeIn, stay, fadeOut);
                    sender.sendMessage(Component.translatable("cloudburst.command.title.times.success", Component.text(args[2]), Component.text(args[3]), Component.text(args[4]), Component.text(player.getName())));
                } catch (NumberFormatException exception) {
                    sender.sendMessage(Component.translatable("commands.generic.exception").color(NamedTextColor.RED));
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
}
