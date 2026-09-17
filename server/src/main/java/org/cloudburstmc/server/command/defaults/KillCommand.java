package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.network.CommandNetworkData;

import java.util.ArrayList;
import java.util.List;

public class KillCommand extends AdvertisedCommand {

    public KillCommand() {
        super("kill", "commands.kill.description", CommandNetworkData.DEFAULT,
                "cloudburst.command.kill.self", "cloudburst.command.kill.other");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.executes(this::executeCommand);
        builder.then(Commands.argument("targets", arguments.entities("target"))
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        if (hasArgument(context, "targets")) {
            List<Entity> targets = CommandArgumentTypes.entities(context, "targets");
            if (targets.isEmpty()) {
                return failure(context, Component.translatable("commands.generic.entity.notFound").color(NamedTextColor.RED));
            }

            if (!hasKillPermission(sender, targets)) {
                return failure(context, Component.translatable("commands.generic.permission").color(NamedTextColor.RED));
            }

            return kill(sender, targets);
        }

        if (sender instanceof Entity entity) {
            if (!sender.hasPermission("cloudburst.command.kill.self")) {
                return failure(context, Component.translatable("commands.generic.permission").color(NamedTextColor.RED));
            }

            return kill(sender, List.of(entity));
        } else {
            return usage();
        }
    }

    private static boolean hasKillPermission(CommandSender sender, List<Entity> targets) {
        if (targets.size() == 1 && targets.getFirst() == sender) {
            return sender.hasPermission("cloudburst.command.kill.self");
        }

        return sender.hasPermission("cloudburst.command.kill.other");
    }

    private static int kill(CommandSender sender, List<Entity> targets) {
        List<Component> killed = new ArrayList<>(targets.size());
        for (Entity target : targets) {
            if (!kill(target)) {
                continue;
            }

            killed.add(target.displayName());
        }

        if (!killed.isEmpty()) {
            Component names = Component.join(JoinConfiguration.separator(Component.text(", ")), killed);
            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.kill.successful", names));
        }

        return killed.size();
    }

    private static boolean kill(Entity target) {
        if (target instanceof Living) {
            target.damage(Float.MAX_VALUE, DamageSource.of(DamageTypes.GENERIC_KILL));
        } else {
            target.kill();
        }

        return !target.isAlive();
    }
}
