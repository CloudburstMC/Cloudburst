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
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.potion.CloudEffect;

import java.util.List;

public class EffectCommand extends AdvertisedCommand {
    public EffectCommand() {
        super("effect", "commands.effect.description", CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.effect");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("player", arguments.entities())
                .then(Commands.argument("clear", CommandArgumentTypes.fixedEnumNamed("effect", "EffectClear", "clear"))
                        .executes(this::executeCommand))
                .then(Commands.argument("effectName", arguments.effect())
                        .executes(this::executeCommand)
                        .then(Commands.argument("seconds", CommandArgumentTypes.integer(0, 1_000_000))
                                .executes(this::executeCommand)
                                .then(Commands.argument("amplifier", CommandArgumentTypes.integer(0, 255))
                                        .executes(this::executeCommand)
                                        .then(Commands.argument("hideParticles", CommandArgumentTypes.bool())
                                                .executes(this::executeCommand))))));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        List<Entity> targets = CommandArgumentTypes.entities(context, "player");
        if (targets.isEmpty()) {
            return success();
        }

        if (hasArgument(context, "clear")) {
            for (Entity target : targets) {
                target.removeAllEffects();
                sender.sendMessage(Component.translatable("commands.effect.success.removed.all",
                        Component.text(target.getName())));
            }
            return success();
        }

        EffectType effectType = argumentValue(context, "effectName", EffectType.class);
        int duration = 300;
        int amplification = 0;
        if (hasArgument(context, "seconds")) {
            duration = argumentValue(context, "seconds", Integer.class);
            if (effectType != EffectTypes.INSTANT_HEALTH && effectType != EffectTypes.INSTANT_DAMAGE) {
                duration *= 20;
            }
        }

        if (hasArgument(context, "amplifier")) {
            amplification = argumentValue(context, "amplifier", Integer.class);
        }

        boolean visible = !hasArgument(context, "hideParticles")
                || !argumentValue(context, "hideParticles", Boolean.class);

        for (Entity target : targets) {
            if (duration == 0) {
                if (!target.hasEffect(effectType)) {
                    if (target.getEffects().isEmpty()) {
                        sender.sendMessage(Component.translatable("commands.effect.failure.notActive.all",
                                Component.text(target.getName())));
                    } else {
                        sender.sendMessage(Component.translatable("commands.effect.failure.notActive",
                                Component.text(effectType.getId().toString()), Component.text(target.getName())));
                    }
                } else {
                    target.removeEffect(effectType);
                    sender.sendMessage(Component.translatable("commands.effect.success.removed",
                            Component.text(effectType.getId().toString()), Component.text(target.getName())));
                }
                continue;
            }

            CloudEffect effect = new CloudEffect(effectType)
                    .setDuration(duration)
                    .setAmplifier(amplification)
                    .setVisible(visible);
            target.addEffect(effect);
            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.effect.success",
                    Component.text(effect.getName()), Component.text(effect.getAmplifier()),
                    Component.text(target.getName()), Component.text(effect.getDuration() / 20)));
        }

        return success();
    }
}
