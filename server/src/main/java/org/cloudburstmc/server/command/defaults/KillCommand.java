package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;

public class KillCommand extends AdvertisedCommand {

    public KillCommand() {
        super("kill", "commands.kill.description", CommandNetworkData.GAME_DIRECTORS,
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
                sender.sendMessage(Component.translatable("commands.generic.entity.notFound").color(NamedTextColor.RED));
                return success();
            }

            if (!hasKillPermission(sender, targets)) {
                sender.sendMessage(Component.translatable("commands.generic.permission").color(NamedTextColor.RED));
                return success();
            }

            for (Entity target : targets) {
                kill(sender, target);
            }

            return success();
        }

        if (sender instanceof CloudPlayer) {
            if (!sender.hasPermission("cloudburst.command.kill.self")) {
                sender.sendMessage(Component.translatable("commands.generic.permission").color(NamedTextColor.RED));
                return success();
            }

            kill(sender, (Entity) sender);
        } else {
            return usage();
        }

        return success();
    }

    private static boolean hasKillPermission(CommandSender sender, List<Entity> targets) {
        if (targets.size() == 1 && targets.getFirst() == sender) {
            return sender.hasPermission("cloudburst.command.kill.self");
        }
        return sender.hasPermission("cloudburst.command.kill.other");
    }

    private static void kill(CommandSender sender, Entity entity) {
        EntityDamageEvent event = new EntityDamageEvent(entity, DamageTypes.SUICIDE, 1000);
        if (!entity.attack(event)) {
            return;
        }
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.kill.successful",
                Component.text(entity.getName())));
    }
}
