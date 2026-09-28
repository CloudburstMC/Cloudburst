package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.resolver.PositionResolver;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.particle.*;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;

import java.awt.*;

public class ParticleCommand extends AdvertisedCommand {
    public ParticleCommand() {
        super("particle", "commands.particle.description", CommandNetworkData.DEFAULT,
                "cloudburst.command.particle");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("effect", CommandArgumentTypes.string())
                .then(Commands.argument("position", CommandArgumentTypes.position()).executes(this::executeCommand)));
        builder.then(Commands.literal("builtin")
                .then(Commands.argument("particle", arguments.particle()).then(positionBranch())));
        builder.then(Commands.literal("block")
                .then(Commands.argument("block", arguments.block()).then(positionBranch())));
        builder.then(Commands.literal("item")
                .then(Commands.argument("item", arguments.item()).then(positionBranch())));
        builder.then(Commands.literal("color")
                .then(Commands.argument("particle", arguments.particle())
                        .then(Commands.argument("red", CommandArgumentTypes.integer(0, 255))
                                .then(Commands.argument("green", CommandArgumentTypes.integer(0, 255))
                                        .then(Commands.argument("blue", CommandArgumentTypes.integer(0, 255))
                                                .then(positionBranch()))))));
        builder.then(Commands.literal("scaled")
                .then(Commands.argument("particle", arguments.particle())
                        .then(Commands.argument("scale", CommandArgumentTypes.integer(0)).then(positionBranch()))));
    }

    private RequiredArgumentBuilder<CommandSourceStack, PositionResolver> positionBranch() {
        return Commands.argument("position", CommandArgumentTypes.position())
                .executes(this::executeCommand)
                .then(Commands.argument("count", CommandArgumentTypes.integer(1, 10000)).executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Level level = context.getSource().level();
        Vector3f position = argumentValue(context, "position", PositionResolver.class).resolve(context.getSource());

        try {
            Identifier id;
            if (hasArgument(context, "effect")) {
                ParticleEmitter emitter = new ParticleEmitter(Identifier.parse(argumentValue(context, "effect", String.class)));
                level.spawnParticleEffect(emitter, position);
                id = emitter.type().getId();
            } else {
                ParticleOptions options = particleOptions(context);
                int count = hasArgument(context, "count") ? argumentValue(context, "count", Integer.class) : 1;
                level.spawnParticle(new ParticleEmission(options, count, Vector3f.ZERO), position);
                id = options.getType().getId();
            }

            sender(context).sendMessage(Component.translatable("commands.particle.success", Component.text(id.toString())));
            return success();
        } catch (IllegalArgumentException exception) {
            return failure(context, Component.text(exception.getMessage()));
        }
    }

    private ParticleOptions particleOptions(CommandContext<CommandSourceStack> context) {
        if (hasArgument(context, "block")) {
            BlockType block = argumentValue(context, "block", BlockType.class);
            return new BlockParticleOptions(ParticleTypes.TERRAIN, block.getDefaultState());
        }

        if (hasArgument(context, "item")) {
            ItemType item = argumentValue(context, "item", ItemType.class);
            return new ItemParticleOptions(ItemStack.builder().itemType(item).build());
        }

        ParticleType type = argumentValue(context, "particle", ParticleType.class);
        if (hasArgument(context, "red")) {
            return new ColoredParticleOptions(type, new Color(
                    argumentValue(context, "red", Integer.class),
                    argumentValue(context, "green", Integer.class),
                    argumentValue(context, "blue", Integer.class)));
        }

        if (hasArgument(context, "scale")) {
            return new ScaledParticleOptions(type, argumentValue(context, "scale", Integer.class));
        }

        return type;
    }
}
