package org.cloudburstmc.server.command.network;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.command.argument.CommandArgumentKind;
import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.protocol.bedrock.data.command.CommandEnumConstraint;

import java.util.*;

/**
 * Maps registry-backed command arguments to command UI enum values.
 */
@UtilityClass
public class CommandRegistryArgumentNetworkMapper {
    private static final String BLOCK_ENUM = "Block";
    private static final String EFFECT_ENUM = "Effect";
    private static final String ENCHANTMENT_ENUM = "enchantmentType";
    private static final String ENTITY_TYPE_ENUM = "entityType";
    private static final String ITEM_ENUM = "Item";
    private static final String PARTICLE_ENUM = "particleType";
    private static final Set<CommandEnumConstraint> ALIAS_CONSTRAINTS =
            Collections.unmodifiableSet(EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES));

    public static String enumName(CommandArgumentType<?> argument) {
        return switch (argument.getKind()) {
            case ITEM -> ITEM_ENUM;
            case BLOCK -> BLOCK_ENUM;
            case ENCHANTMENT -> ENCHANTMENT_ENUM;
            case EFFECT -> EFFECT_ENUM;
            case ENTITY_TYPE -> ENTITY_TYPE_ENUM;
            case PARTICLE -> PARTICLE_ENUM;
            default -> throw new IllegalArgumentException("Not a registry command argument: " + argument.getKind());
        };
    }

    public static Map<String, Set<CommandEnumConstraint>> enumValues(CommandArgumentType<?> argument) {
        if (!isRegistryArgument(argument.getKind())) {
            throw new IllegalArgumentException("Not a registry command argument: " + argument.getKind());
        }
        return enumValues(argument.getKind(), argument.getValues());
    }

    public static boolean requiresSemanticConstraint(CommandArgumentKind kind) {
        return kind == CommandArgumentKind.ITEM || kind == CommandArgumentKind.BLOCK;
    }

    private static boolean isRegistryArgument(CommandArgumentKind kind) {
        return switch (kind) {
            case ITEM, BLOCK, ENCHANTMENT, EFFECT, ENTITY_TYPE, PARTICLE -> true;
            default -> false;
        };
    }

    private static Map<String, Set<CommandEnumConstraint>> enumValues(CommandArgumentKind kind, Collection<String> identifiers) {
        LinkedHashMap<String, Set<CommandEnumConstraint>> enumValues = new LinkedHashMap<>();
        for (String identifier : identifiers) {
            String shorthand = minecraftShorthand(identifier);
            Set<CommandEnumConstraint> constraints = shorthand != null && requiresSemanticConstraint(kind)
                    ? ALIAS_CONSTRAINTS
                    : Collections.emptySet();
            enumValues.put(identifier, constraints);
            if (shorthand != null) {
                enumValues.put(shorthand, Collections.emptySet());
            }
        }

        return enumValues;
    }

    private static String minecraftShorthand(String identifier) {
        int separator = identifier.indexOf(':');
        if (separator <= 0) {
            return null;
        }

        return "minecraft".equals(identifier.substring(0, separator)) ? identifier.substring(separator + 1) : null;
    }
}
