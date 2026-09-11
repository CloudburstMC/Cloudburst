package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.CloudCommandSourceStack;
import org.cloudburstmc.server.command.CloudConsoleCommandSender;
import org.cloudburstmc.server.command.ServerCommand;
import org.cloudburstmc.server.registry.CloudCommandRegistry;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class HelpCommand extends ServerCommand {

    public HelpCommand() {
        super("help", "commands.help.description", List.of(), List.of("cloudburst.command.help"));
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.executes(this::executeCommand);
        builder.then(Commands.argument("page", CommandArgumentTypes.integer(1))
                .executes(this::executeCommand));
        builder.then(Commands.argument("command", CommandArgumentTypes.string())
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        int pageNumber = 1;
        int pageHeight = 5;
        String command = "";
        if (hasArgument(context, "page")) {
            pageNumber = argumentValue(context, "page", Integer.class);
        } else if (hasArgument(context, "command")) {
            command = argumentValue(context, "command");
        }

        if (sender instanceof CloudConsoleCommandSender) {
            pageHeight = Integer.MAX_VALUE;
        }

        if (command.isEmpty()) {
            Map<String, String> commands = new TreeMap<>(((CloudServer) sender.getServer()).getCommandRegistry().visibleCommands(sender));
            int totalPage = commands.size() % pageHeight == 0 ? commands.size() / pageHeight : commands.size() / pageHeight + 1;
            pageNumber = Math.min(pageNumber, totalPage);
            if (pageNumber < 1) {
                pageNumber = 1;
            }

            sender.sendMessage(Component.translatable("commands.help.header",
                    Component.text(pageNumber), Component.text(totalPage)));
            int i = 1;
            for (Map.Entry<String, String> entry : commands.entrySet()) {
                if (i >= (pageNumber - 1) * pageHeight + 1 && i <= Math.min(commands.size(), pageNumber * pageHeight)) {
                    String desc = ((CloudServer) sender.getServer()).getLanguage().translate(entry.getValue());
                    sender.sendMessage(Component.text("/" + entry.getKey() + ": ").color(NamedTextColor.DARK_GREEN)
                            .append(Component.text(desc).color(NamedTextColor.WHITE)));
                }
                i++;
            }
        } else {
            CloudCommandRegistry registry = ((CloudServer) sender.getServer()).getCommandRegistry();
            String label = command.toLowerCase(Locale.ROOT);
            CloudCommandSourceStack source = CloudCommandSourceStack.from(sender);
            if (registry.canUse(label, source)) {
                sendHelp(sender, label, registry.commandDescription(label), registry.usages(label, source));
            } else {
                sender.sendMessage(Component.text("No help for " + command).color(NamedTextColor.RED));
            }
        }

        return success();
    }

    private static void sendHelp(
            CommandSender sender,
            String command,
            String descriptionKey,
            Iterable<String> usages
    ) {
        String desc = ((CloudServer) sender.getServer()).getLanguage().translate(descriptionKey);
        sender.sendMessage(Component.text(" Help: /" + command + " ").color(NamedTextColor.WHITE)
                .append(Component.text("\nDescription: ").color(NamedTextColor.GOLD))
                .append(Component.text(desc).color(NamedTextColor.WHITE)));
        for (String usage : usages) {
            sender.sendMessage(Component.text("Usage: ").color(NamedTextColor.GOLD)
                    .append(Component.text(usage).color(NamedTextColor.WHITE)));
        }
    }
}
