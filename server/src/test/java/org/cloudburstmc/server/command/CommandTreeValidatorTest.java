package org.cloudburstmc.server.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandTreeValidatorTest {

    @Test
    void acceptsRepresentableExecutableTree() {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test")
                .then(Commands.argument("value", CommandArgumentTypes.integer()).executes(context -> 1))
                .build();

        assertDoesNotThrow(() -> CommandTreeValidator.validate(root));
    }

    @Test
    void rejectsArgumentWithoutClientRepresentation() {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test")
                .then(com.mojang.brigadier.builder.RequiredArgumentBuilder
                        .<CommandSourceStack, Integer>argument("value", IntegerArgumentType.integer())
                        .executes(context -> 1))
                .build();

        assertThrows(IllegalArgumentException.class, () -> CommandTreeValidator.validate(root));
    }

    @Test
    void acceptsRedirectsToRepresentableTrees() {
        LiteralCommandNode<CommandSourceStack> target = Commands.literal("target").executes(context -> 1).build();
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test").then(Commands.literal("redirect").redirect(target)).build();

        assertDoesNotThrow(() -> CommandTreeValidator.validate(root));
    }

    @Test
    void rejectsTreeWithoutExecutableNode() {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test").then(Commands.literal("child")).build();

        assertThrows(IllegalArgumentException.class, () -> CommandTreeValidator.validate(root));
    }
}
