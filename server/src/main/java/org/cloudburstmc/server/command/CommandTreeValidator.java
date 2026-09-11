package org.cloudburstmc.server.command;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.argument.CommandArgumentType;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Validates that a Brigadier tree has an exact representation in command data sent to clients.
 */
@UtilityClass
public class CommandTreeValidator {

    public static void validate(CommandNode<CommandSourceStack> root) {
        validateNode(root, Collections.newSetFromMap(new IdentityHashMap<>()));
        if (!hasExecutableNode(root, Collections.newSetFromMap(new IdentityHashMap<>()))) {
            throw new IllegalArgumentException("Command tree has no executable node: " + root.getName());
        }
    }

    private static void validateNode(CommandNode<CommandSourceStack> node, Set<CommandNode<CommandSourceStack>> visiting) {
        if (!visiting.add(node)) {
            throw new IllegalArgumentException("Command tree contains a redirect cycle: " + node.getName());
        }

        if (node instanceof ArgumentCommandNode<CommandSourceStack, ?> argument && !(argument.getType() instanceof CommandArgumentType<?>)) {
            throw new IllegalArgumentException("Command argument has no client representation: " + node.getName());
        }

        if (node.getRedirect() != null) {
            validateNode(node.getRedirect(), visiting);
        }

        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            validateNode(child, visiting);
        }

        visiting.remove(node);
    }

    private static boolean hasExecutableNode(CommandNode<CommandSourceStack> node, Set<CommandNode<CommandSourceStack>> visiting) {
        if (!visiting.add(node)) {
            return false;
        }

        try {
            if (node.getCommand() != null) {
                return true;
            }

            if (node.getRedirect() != null && hasExecutableNode(node.getRedirect(), visiting)) {
                return true;
            }

            for (CommandNode<CommandSourceStack> child : node.getChildren()) {
                if (hasExecutableNode(child, visiting)) {
                    return true;
                }
            }

            return false;
        } finally {
            visiting.remove(node);
        }
    }
}
