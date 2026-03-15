package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.ConsoleCommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.registry.CommandRegistry;

import java.util.Map;
import java.util.TreeMap;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", CommandData.builder("help")
                .setDescription("commands.help.description")
                .setUsageMessage("/help [command|page]")
                .addAlias("?")
                .setPermissions("cloudburst.command.help")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("page", CommandParamType.INT, true)
                }, new CommandParameter[]{
                        new CommandParameter("command", CommandParamType.COMMAND, true)
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        int pageNumber = 1;
        int pageHeight = 5;
        String command = "";
        if (args.length != 0) {
            try {
                pageNumber = Integer.parseInt(args[0]);
                if (pageNumber <= 0) {
                    pageNumber = 1;
                }
            } catch (NumberFormatException e) {
                command = args[0];
            }
        }

        if (sender instanceof ConsoleCommandSender) {
            pageHeight = Integer.MAX_VALUE;
        }

        if (command.length() == 0) {
            Map<String, Command> commands = new TreeMap<>();
            for (Command cmd : CommandRegistry.get().getRegisteredCommands().values()) {
                if (cmd.testPermissionSilent(sender)) {
                    commands.put(cmd.getName(), cmd);
                }
            }
            int totalPage = commands.size() % pageHeight == 0 ? commands.size() / pageHeight : commands.size() / pageHeight + 1;
            pageNumber = Math.min(pageNumber, totalPage);
            if (pageNumber < 1) {
                pageNumber = 1;
            }

            sender.sendMessage(Component.translatable("commands.help.header",
                    Component.text(pageNumber), Component.text(totalPage)));
            int i = 1;
            for (Command command1 : commands.values()) {
                if (i >= (pageNumber - 1) * pageHeight + 1 && i <= Math.min(commands.size(), pageNumber * pageHeight)) {
                    String desc = ((CloudServer) sender.getServer()).getLanguage().translate(command1.getDescription());
                    sender.sendMessage(Component.text("/" + command1.getName() + ": ").color(NamedTextColor.DARK_GREEN)
                            .append(Component.text(desc).color(NamedTextColor.WHITE)));
                }
                i++;
            }
        } else {
            Command cmd = CommandRegistry.get().getCommand(command.toLowerCase());
            if (cmd != null) {
                if (cmd.testPermissionSilent(sender)) {
                    String desc = ((CloudServer) sender.getServer()).getLanguage().translate(cmd.getDescription());
                    sender.sendMessage(Component.text(" Help: /" + cmd.getName() + " ").color(NamedTextColor.WHITE)
                            .append(Component.text("\nDescription: ").color(NamedTextColor.GOLD))
                            .append(Component.text(desc).color(NamedTextColor.WHITE))
                            .append(Component.text("\nUsage: ").color(NamedTextColor.GOLD))
                            .append(Component.text(cmd.getUsage()).color(NamedTextColor.WHITE)));
                    return true;
                }
            }
            sender.sendMessage(Component.text("No help for " + command).color(NamedTextColor.RED));
        }
        return true;
    }
}
