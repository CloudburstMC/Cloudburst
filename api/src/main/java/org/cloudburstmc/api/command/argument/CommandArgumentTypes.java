package org.cloudburstmc.api.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.argument.resolver.*;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.particle.ParticleType;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.registry.KeyedRegistry;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory methods for command argument types with command UI metadata.
 */
@UtilityClass
public class CommandArgumentTypes {
    private static final Pattern RELATIVE_COORDINATE_PATTERN =
            Pattern.compile("(~)?([+\\-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+))?");
    private static final DynamicCommandExceptionType INVALID_ENUM_VALUE =
            new DynamicCommandExceptionType(value -> new LiteralMessage("Invalid command argument value: " + value));
    private static final SimpleCommandExceptionType ERROR_NOT_SINGLE_ENTITY =
            new SimpleCommandExceptionType(new LiteralMessage("Selector matched more than one entity"));
    private static final SimpleCommandExceptionType ERROR_NOT_SINGLE_PLAYER =
            new SimpleCommandExceptionType(new LiteralMessage("Selector matched more than one player"));

    /**
     * Creates an argument that parses one unquoted string token.
     *
     * @return a string argument
     */
    public static CommandArgumentType<String> string() {
        return string(null);
    }

    /**
     * Creates an argument that parses one unquoted string token.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a string argument
     */
    public static CommandArgumentType<String> string(@Nullable String displayName) {
        return argument(CommandArgumentKind.STRING, displayName, null, List.of(), null);
    }

    /**
     * Creates an argument that parses the rest of the input as text.
     *
     * @return a text argument
     */
    public static CommandArgumentType<String> text() {
        return text(null);
    }

    /**
     * Creates an argument that parses the rest of the input as text.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a text argument
     */
    public static CommandArgumentType<String> text(@Nullable String displayName) {
        return argument(CommandArgumentKind.TEXT, displayName, null, List.of(), null);
    }

    /**
     * Creates an argument that parses the rest of the input as a chat message.
     *
     * @return a message argument
     */
    public static CommandArgumentType<String> message() {
        return message(null);
    }

    /**
     * Creates an argument that parses the rest of the input as a chat message.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a message argument
     */
    public static CommandArgumentType<String> message(@Nullable String displayName) {
        return argument(CommandArgumentKind.MESSAGE, displayName, null, List.of(), null);
    }

    /**
     * Creates an argument that parses {@code true} or {@code false}.
     *
     * @return a boolean argument
     */
    public static CommandArgumentType<Boolean> bool() {
        return bool(null);
    }

    /**
     * Creates an argument that parses {@code true} or {@code false}.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a boolean argument
     */
    public static CommandArgumentType<Boolean> bool(@Nullable String displayName) {
        return fixedEnumMapped(displayName, "Boolean", Boolean::valueOf, "false", "true");
    }

    /**
     * Creates an argument that parses a signed integer.
     *
     * @return an integer argument
     */
    public static CommandArgumentType<Integer> integer() {
        return integer(null, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Creates an argument that parses a signed integer.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return an integer argument
     */
    public static CommandArgumentType<Integer> integer(@Nullable String displayName) {
        return integer(displayName, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Creates an integer argument with a minimum accepted value.
     *
     * @param minimum minimum accepted value
     * @return a bounded integer argument
     */
    public static CommandArgumentType<Integer> integer(int minimum) {
        return integer(null, minimum, Integer.MAX_VALUE);
    }

    /**
     * Creates an integer argument with a minimum accepted value.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param minimum     minimum accepted value
     * @return a bounded integer argument
     */
    public static CommandArgumentType<Integer> integer(@Nullable String displayName, int minimum) {
        return integer(displayName, minimum, Integer.MAX_VALUE);
    }

    /**
     * Creates an integer argument with an inclusive accepted range.
     *
     * @param minimum minimum accepted value
     * @param maximum maximum accepted value
     * @return a bounded integer argument
     */
    public static CommandArgumentType<Integer> integer(int minimum, int maximum) {
        return integer(null, minimum, maximum);
    }

    /**
     * Creates an integer argument with an inclusive accepted range.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param minimum     minimum accepted value
     * @param maximum     maximum accepted value
     * @return a bounded integer argument
     */
    public static CommandArgumentType<Integer> integer(@Nullable String displayName, int minimum, int maximum) {
        return new NumericCommandArgumentType<>(CommandArgumentKind.INTEGER, displayName,
                IntegerArgumentType.integer(minimum, maximum));
    }

    /**
     * Creates an argument that parses a floating-point number.
     *
     * @return a floating-point argument
     */
    public static CommandArgumentType<Float> floatingPoint() {
        return floatingPoint(null, -Float.MAX_VALUE, Float.MAX_VALUE);
    }

    /**
     * Creates an argument that parses a floating-point number.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a floating-point argument
     */
    public static CommandArgumentType<Float> floatingPoint(@Nullable String displayName) {
        return floatingPoint(displayName, -Float.MAX_VALUE, Float.MAX_VALUE);
    }

    /**
     * Creates a floating-point argument with a minimum accepted value.
     *
     * @param minimum minimum accepted value
     * @return a bounded floating-point argument
     */
    public static CommandArgumentType<Float> floatingPoint(float minimum) {
        return floatingPoint(null, minimum, Float.MAX_VALUE);
    }

    /**
     * Creates a floating-point argument with a minimum accepted value.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param minimum     minimum accepted value
     * @return a bounded floating-point argument
     */
    public static CommandArgumentType<Float> floatingPoint(@Nullable String displayName, float minimum) {
        return floatingPoint(displayName, minimum, Float.MAX_VALUE);
    }

    /**
     * Creates a floating-point argument with an inclusive accepted range.
     *
     * @param minimum minimum accepted value
     * @param maximum maximum accepted value
     * @return a bounded floating-point argument
     */
    public static CommandArgumentType<Float> floatingPoint(float minimum, float maximum) {
        return floatingPoint(null, minimum, maximum);
    }

    /**
     * Creates a floating-point argument with an inclusive accepted range.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param minimum     minimum accepted value
     * @param maximum     maximum accepted value
     * @return a bounded floating-point argument
     */
    public static CommandArgumentType<Float> floatingPoint(@Nullable String displayName, float minimum, float maximum) {
        return new NumericCommandArgumentType<>(CommandArgumentKind.FLOAT, displayName,
                FloatArgumentType.floatArg(minimum, maximum));
    }

    /**
     * Creates an argument that parses an absolute or relative rotation value.
     *
     * @return a rotation argument
     */
    public static CommandArgumentType<RotationResolver> rotation() {
        return rotation(null);
    }

    /**
     * Creates an argument that parses an absolute or relative rotation value.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a rotation argument
     */
    public static CommandArgumentType<RotationResolver> rotation(@Nullable String displayName) {
        return new SimpleCommandArgumentType<>(CommandArgumentKind.ROTATION, displayName, null, List.of(), null,
                CommandArgumentTypes::parseRotation);
    }

    /**
     * Resolves a single-player selector argument from a command context.
     *
     * @param context the command context
     * @param name    the argument node name
     * @return the resolved player
     * @throws CommandSyntaxException if the argument cannot resolve to one player
     */
    public static Player player(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        List<Player> players = players(context, name);
        if (players.size() != 1) {
            throw ERROR_NOT_SINGLE_PLAYER.create();
        }
        return players.getFirst();
    }

    /**
     * Resolves a player selector argument from a command context.
     *
     * @param context the command context
     * @param name    the argument node name
     * @return the resolved players
     * @throws CommandSyntaxException if the argument cannot be resolved
     */
    public static List<Player> players(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, PlayerSelectorResolver.class).resolve(context.getSource());
    }

    /**
     * Resolves a single-entity selector argument from a command context.
     *
     * @param context the command context
     * @param name    the argument node name
     * @return the resolved entity
     * @throws CommandSyntaxException if the argument cannot resolve to one entity
     */
    public static Entity entity(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        List<Entity> entities = entities(context, name);
        if (entities.size() != 1) {
            throw ERROR_NOT_SINGLE_ENTITY.create();
        }
        return entities.getFirst();
    }

    /**
     * Resolves an entity selector argument from a command context.
     *
     * @param context the command context
     * @param name    the argument node name
     * @return the resolved entities
     * @throws CommandSyntaxException if the argument cannot be resolved
     */
    public static List<Entity> entities(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, EntitySelectorResolver.class).resolve(context.getSource());
    }

    /**
     * Creates an argument that resolves three coordinates to a precise position.
     *
     * @return a position resolver argument
     */
    public static CommandArgumentType<PositionResolver> position() {
        return position(null);
    }

    /**
     * Creates an argument that resolves three coordinates to a precise position.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a position resolver argument
     */
    public static CommandArgumentType<PositionResolver> position(@Nullable String displayName) {
        return argument(CommandArgumentKind.POSITION, displayName, null, List.of(), null);
    }

    /**
     * Creates an argument that resolves three coordinates to a block position.
     *
     * @return a block position resolver argument
     */
    public static CommandArgumentType<BlockPositionResolver> blockPosition() {
        return blockPosition(null);
    }

    /**
     * Creates an argument that resolves three coordinates to a block position.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a block position resolver argument
     */
    public static CommandArgumentType<BlockPositionResolver> blockPosition(@Nullable String displayName) {
        return argument(CommandArgumentKind.BLOCK_POSITION, displayName, null, List.of(), null);
    }

    /**
     * Creates an argument that parses the rest of the input as JSON text.
     *
     * @return a JSON argument
     */
    public static CommandArgumentType<String> json() {
        return json(null);
    }

    /**
     * Creates an argument that parses the rest of the input as JSON text.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a JSON argument
     */
    public static CommandArgumentType<String> json(@Nullable String displayName) {
        return argument(CommandArgumentKind.JSON, displayName, null, List.of(), null);
    }

    /**
     * Creates an item registry argument using the built-in item registry.
     *
     * @return an item argument
     */
    public static CommandArgumentType<ItemType> item() {
        return item((String) null);
    }

    /**
     * Creates an item registry argument using the built-in item registry.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return an item argument
     */
    public static CommandArgumentType<ItemType> item(@Nullable String displayName) {
        return registryArgument(CommandArgumentKind.ITEM, displayName, ItemTypes::get,
                identifiers(ItemTypes.values(), ItemType::getId), "item");
    }

    /**
     * Creates an item registry argument using a keyed registry for lookup and suggestions.
     *
     * @param registry the registry backing this argument
     * @return an item argument
     */
    public static CommandArgumentType<ItemType> item(KeyedRegistry<ItemType> registry) {
        return item(null, registry);
    }

    /**
     * Creates an item registry argument using a keyed registry for lookup and suggestions.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param registry    the registry backing this argument
     * @return an item argument
     */
    public static CommandArgumentType<ItemType> item(@Nullable String displayName, KeyedRegistry<ItemType> registry) {
        return registryArgument(CommandArgumentKind.ITEM, displayName, registry, "item");
    }

    /**
     * Creates an item registry argument using a custom lookup.
     *
     * @param lookup resolves parsed identifiers to item types
     * @return an item argument
     */
    public static CommandArgumentType<ItemType> item(Function<Identifier, Optional<ItemType>> lookup) {
        return item(null, lookup);
    }

    /**
     * Creates an item registry argument using a custom lookup.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to item types
     * @return an item argument
     */
    public static CommandArgumentType<ItemType> item(@Nullable String displayName, Function<Identifier, Optional<ItemType>> lookup) {
        return registryArgument(CommandArgumentKind.ITEM, displayName, lookup, List.of(), "item");
    }

    /**
     * Creates an item registry argument using a custom lookup and suggestion list.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to item types
     * @param suggestions identifiers suggested by command completions
     * @return an item argument
     */
    public static CommandArgumentType<ItemType> item(@Nullable String displayName, Function<Identifier, Optional<ItemType>> lookup, Collection<Identifier> suggestions) {
        return registryArgument(CommandArgumentKind.ITEM, displayName, lookup, identifiers(suggestions), "item");
    }

    /**
     * Creates a block registry argument using the built-in block registry.
     *
     * @return a block argument
     */
    public static CommandArgumentType<BlockType> block() {
        return block((String) null);
    }

    /**
     * Creates a block registry argument using the built-in block registry.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a block argument
     */
    public static CommandArgumentType<BlockType> block(@Nullable String displayName) {
        return registryArgument(CommandArgumentKind.BLOCK, displayName, BlockTypes::get, identifiers(BlockTypes.values(), BlockType::getId), "block");
    }

    /**
     * Creates a block registry argument using a keyed registry for lookup and suggestions.
     *
     * @param registry the registry backing this argument
     * @return a block argument
     */
    public static CommandArgumentType<BlockType> block(KeyedRegistry<BlockType> registry) {
        return block(null, registry);
    }

    /**
     * Creates a block registry argument using a keyed registry for lookup and suggestions.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param registry    the registry backing this argument
     * @return a block argument
     */
    public static CommandArgumentType<BlockType> block(@Nullable String displayName, KeyedRegistry<BlockType> registry) {
        return registryArgument(CommandArgumentKind.BLOCK, displayName, registry, "block");
    }

    /**
     * Creates a block registry argument using a custom lookup.
     *
     * @param lookup resolves parsed identifiers to block types
     * @return a block argument
     */
    public static CommandArgumentType<BlockType> block(Function<Identifier, Optional<BlockType>> lookup) {
        return block(null, lookup);
    }

    /**
     * Creates a block registry argument using a custom lookup.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to block types
     * @return a block argument
     */
    public static CommandArgumentType<BlockType> block(@Nullable String displayName, Function<Identifier, Optional<BlockType>> lookup) {
        return registryArgument(CommandArgumentKind.BLOCK, displayName, lookup, List.of(), "block");
    }

    /**
     * Creates a block registry argument using a custom lookup and suggestion list.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to block types
     * @param suggestions identifiers suggested by command completions
     * @return a block argument
     */
    public static CommandArgumentType<BlockType> block(@Nullable String displayName, Function<Identifier, Optional<BlockType>> lookup, Collection<Identifier> suggestions) {
        return registryArgument(CommandArgumentKind.BLOCK, displayName, lookup, identifiers(suggestions), "block");
    }

    /**
     * Creates an enchantment registry argument using the built-in enchantment registry.
     *
     * @return an enchantment argument
     */
    public static CommandArgumentType<EnchantmentType> enchantment() {
        return enchantment((String) null);
    }

    /**
     * Creates an enchantment registry argument using the built-in enchantment registry.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return an enchantment argument
     */
    public static CommandArgumentType<EnchantmentType> enchantment(@Nullable String displayName) {
        return registryArgument(CommandArgumentKind.ENCHANTMENT, displayName, EnchantmentTypes::get, identifiers(EnchantmentTypes.values(), EnchantmentType::identifier), "enchantment");
    }

    /**
     * Creates an enchantment registry argument using a keyed registry for lookup and suggestions.
     *
     * @param registry the registry backing this argument
     * @return an enchantment argument
     */
    public static CommandArgumentType<EnchantmentType> enchantment(KeyedRegistry<EnchantmentType> registry) {
        return enchantment(null, registry);
    }

    /**
     * Creates an enchantment registry argument using a keyed registry for lookup and suggestions.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param registry    the registry backing this argument
     * @return an enchantment argument
     */
    public static CommandArgumentType<EnchantmentType> enchantment(@Nullable String displayName, KeyedRegistry<EnchantmentType> registry) {
        return registryArgument(CommandArgumentKind.ENCHANTMENT, displayName, registry, "enchantment");
    }

    /**
     * Creates an enchantment registry argument using a custom lookup.
     *
     * @param lookup resolves parsed identifiers to enchantment types
     * @return an enchantment argument
     */
    public static CommandArgumentType<EnchantmentType> enchantment(Function<Identifier, Optional<EnchantmentType>> lookup) {
        return enchantment(null, lookup);
    }

    /**
     * Creates an enchantment registry argument using a custom lookup.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to enchantment types
     * @return an enchantment argument
     */
    public static CommandArgumentType<EnchantmentType> enchantment(@Nullable String displayName, Function<Identifier, Optional<EnchantmentType>> lookup) {
        return registryArgument(CommandArgumentKind.ENCHANTMENT, displayName, lookup, List.of(), "enchantment");
    }

    /**
     * Creates an enchantment registry argument using a custom lookup and suggestion list.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to enchantment types
     * @param suggestions identifiers suggested by command completions
     * @return an enchantment argument
     */
    public static CommandArgumentType<EnchantmentType> enchantment(@Nullable String displayName, Function<Identifier, Optional<EnchantmentType>> lookup, Collection<Identifier> suggestions) {
        return registryArgument(CommandArgumentKind.ENCHANTMENT, displayName, lookup, identifiers(suggestions), "enchantment");
    }

    /**
     * Creates an effect registry argument using the built-in effect registry.
     *
     * @return an effect argument
     */
    public static CommandArgumentType<EffectType> effect() {
        return effect((String) null);
    }

    /**
     * Creates an effect registry argument using the built-in effect registry.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return an effect argument
     */
    public static CommandArgumentType<EffectType> effect(@Nullable String displayName) {
        return registryArgument(CommandArgumentKind.EFFECT, displayName, EffectTypes::get, identifiers(EffectTypes.values(), EffectType::getId), "effect");
    }

    /**
     * Creates an effect registry argument using a keyed registry for lookup and suggestions.
     *
     * @param registry the registry backing this argument
     * @return an effect argument
     */
    public static CommandArgumentType<EffectType> effect(KeyedRegistry<EffectType> registry) {
        return effect(null, registry);
    }

    /**
     * Creates an effect registry argument using a keyed registry for lookup and suggestions.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param registry    the registry backing this argument
     * @return an effect argument
     */
    public static CommandArgumentType<EffectType> effect(@Nullable String displayName, KeyedRegistry<EffectType> registry) {
        return registryArgument(CommandArgumentKind.EFFECT, displayName, registry, "effect");
    }

    /**
     * Creates an effect registry argument using a custom lookup.
     *
     * @param lookup resolves parsed identifiers to effect types
     * @return an effect argument
     */
    public static CommandArgumentType<EffectType> effect(Function<Identifier, Optional<EffectType>> lookup) {
        return effect(null, lookup);
    }

    /**
     * Creates an effect registry argument using a custom lookup.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to effect types
     * @return an effect argument
     */
    public static CommandArgumentType<EffectType> effect(@Nullable String displayName, Function<Identifier, Optional<EffectType>> lookup) {
        return registryArgument(CommandArgumentKind.EFFECT, displayName, lookup, List.of(), "effect");
    }

    /**
     * Creates an effect registry argument using a custom lookup and suggestion list.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to effect types
     * @param suggestions identifiers suggested by command completions
     * @return an effect argument
     */
    public static CommandArgumentType<EffectType> effect(@Nullable String displayName, Function<Identifier, Optional<EffectType>> lookup, Collection<Identifier> suggestions) {
        return registryArgument(CommandArgumentKind.EFFECT, displayName, lookup, identifiers(suggestions), "effect");
    }

    /**
     * Creates an entity type registry argument using the built-in entity type registry.
     *
     * @return an entity type argument
     */
    public static CommandArgumentType<EntityType<?>> entityType() {
        return entityType((String) null);
    }

    /**
     * Creates an entity type registry argument using the built-in entity type registry.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return an entity type argument
     */
    public static CommandArgumentType<EntityType<?>> entityType(@Nullable String displayName) {
        return registryArgument(CommandArgumentKind.ENTITY_TYPE, displayName, EntityTypes::get,
                identifiers(EntityTypes.values(), EntityType::getId), "entity");
    }

    /**
     * Creates an entity type registry argument using a keyed registry for lookup and suggestions.
     *
     * @param registry the registry backing this argument
     * @return an entity type argument
     */
    public static CommandArgumentType<EntityType<?>> entityType(KeyedRegistry<EntityType<?>> registry) {
        return entityType(null, registry);
    }

    /**
     * Creates an entity type registry argument using a keyed registry for lookup and suggestions.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param registry    the registry backing this argument
     * @return an entity type argument
     */
    public static CommandArgumentType<EntityType<?>> entityType(@Nullable String displayName, KeyedRegistry<EntityType<?>> registry) {
        return registryArgument(CommandArgumentKind.ENTITY_TYPE, displayName, registry, "entity");
    }

    /**
     * Creates an entity type registry argument using a custom lookup.
     *
     * @param lookup resolves parsed identifiers to entity types
     * @return an entity type argument
     */
    public static CommandArgumentType<EntityType<?>> entityType(Function<Identifier, Optional<EntityType<?>>> lookup) {
        return entityType(null, lookup);
    }

    /**
     * Creates an entity type registry argument using a custom lookup.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to entity types
     * @return an entity type argument
     */
    public static CommandArgumentType<EntityType<?>> entityType(@Nullable String displayName, Function<Identifier, Optional<EntityType<?>>> lookup) {
        return registryArgument(CommandArgumentKind.ENTITY_TYPE, displayName, lookup, List.of(), "entity");
    }

    /**
     * Creates an entity type registry argument using a custom lookup and suggestion list.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to entity types
     * @param suggestions identifiers suggested by command completions
     * @return an entity type argument
     */
    public static CommandArgumentType<EntityType<?>> entityType(@Nullable String displayName, Function<Identifier, Optional<EntityType<?>>> lookup, Collection<Identifier> suggestions) {
        return registryArgument(CommandArgumentKind.ENTITY_TYPE, displayName, lookup, identifiers(suggestions), "entity");
    }

    /**
     * Creates a particle registry argument using the built-in particle registry.
     *
     * @return a particle argument
     */
    public static CommandArgumentType<ParticleType> particle() {
        return particle((String) null);
    }

    /**
     * Creates a particle registry argument using the built-in particle registry.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @return a particle argument
     */
    public static CommandArgumentType<ParticleType> particle(@Nullable String displayName) {
        return registryArgument(CommandArgumentKind.PARTICLE, displayName, ParticleTypes::get, identifiers(ParticleTypes.values(), ParticleType::id), "particle");
    }

    /**
     * Creates a particle registry argument using a keyed registry for lookup and suggestions.
     *
     * @param registry the registry backing this argument
     * @return a particle argument
     */
    public static CommandArgumentType<ParticleType> particle(KeyedRegistry<ParticleType> registry) {
        return particle(null, registry);
    }

    /**
     * Creates a particle registry argument using a keyed registry for lookup and suggestions.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param registry    the registry backing this argument
     * @return a particle argument
     */
    public static CommandArgumentType<ParticleType> particle(@Nullable String displayName, KeyedRegistry<ParticleType> registry) {
        return registryArgument(CommandArgumentKind.PARTICLE, displayName, registry, "particle");
    }

    /**
     * Creates a particle registry argument using a custom lookup.
     *
     * @param lookup resolves parsed identifiers to particle types
     * @return a particle argument
     */
    public static CommandArgumentType<ParticleType> particle(Function<Identifier, Optional<ParticleType>> lookup) {
        return particle(null, lookup);
    }

    /**
     * Creates a particle registry argument using a custom lookup.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to particle types
     * @return a particle argument
     */
    public static CommandArgumentType<ParticleType> particle(@Nullable String displayName, Function<Identifier, Optional<ParticleType>> lookup) {
        return registryArgument(CommandArgumentKind.PARTICLE, displayName, lookup, List.of(), "particle");
    }

    /**
     * Creates a particle registry argument using a custom lookup and suggestion list.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param lookup      resolves parsed identifiers to particle types
     * @param suggestions identifiers suggested by command completions
     * @return a particle argument
     */
    public static CommandArgumentType<ParticleType> particle(@Nullable String displayName, Function<Identifier, Optional<ParticleType>> lookup, Collection<Identifier> suggestions) {
        return registryArgument(CommandArgumentKind.PARTICLE, displayName, lookup, identifiers(suggestions), "particle");
    }

    /**
     * Creates an argument backed by a fixed set of literal values.
     *
     * @param enumName command UI enum name
     * @param values   accepted values
     * @return a fixed enum argument
     */
    public static CommandArgumentType<String> fixedEnum(String enumName, String... values) {
        return fixedEnumNamed(null, enumName, values);
    }

    /**
     * Creates an argument backed by a fixed set of literal values.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param enumName    command UI enum name
     * @param values      accepted values
     * @return a fixed enum argument
     */
    public static CommandArgumentType<String> fixedEnumNamed(@Nullable String displayName, String enumName, String... values) {
        return argument(CommandArgumentKind.FIXED_ENUM, displayName, enumName, List.of(values), null);
    }

    /**
     * Creates an argument backed by a fixed set of literal values and maps the parsed value.
     *
     * @param enumName command UI enum name
     * @param parser   maps the parsed value
     * @param values   accepted values
     * @param <T>      parsed result type
     * @return a fixed enum argument
     */
    public static <T> CommandArgumentType<T> fixedEnumMapped(String enumName, Function<String, T> parser, String... values) {
        return fixedEnumMapped(null, enumName, parser, values);
    }

    /**
     * Creates an argument backed by a fixed set of literal values and maps the parsed value.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param enumName    command UI enum name
     * @param parser      maps the parsed value
     * @param values      accepted values
     * @param <T>         parsed result type
     * @return a fixed enum argument
     */
    public static <T> CommandArgumentType<T> fixedEnumMapped(@Nullable String displayName, String enumName, Function<String, T> parser, String... values) {
        return new SimpleCommandArgumentType<>(CommandArgumentKind.FIXED_ENUM, displayName, enumName, List.of(values), null, parser::apply);
    }

    /**
     * Creates an argument that requires a suffix on the parsed token.
     *
     * @param postfix required suffix
     * @return a postfix argument
     */
    public static CommandArgumentType<String> postfix(String postfix) {
        return postfix(null, postfix);
    }

    /**
     * Creates an argument that requires a suffix on the parsed token.
     *
     * @param displayName the command UI argument name, or {@code null} to use the node name
     * @param postfix     required suffix
     * @return a postfix argument
     */
    public static CommandArgumentType<String> postfix(@Nullable String displayName, String postfix) {
        return argument(CommandArgumentKind.POSTFIX, displayName, null, List.of(), postfix);
    }

    private static <T> CommandArgumentType<T> argument(CommandArgumentKind kind, @Nullable String displayName, @Nullable String enumName, List<String> values, @Nullable String postfix) {
        return new SimpleCommandArgumentType<>(kind, displayName, enumName, values, postfix);
    }

    private static <T> CommandArgumentType<T> registryArgument(CommandArgumentKind kind, @Nullable String displayName, Function<Identifier, Optional<T>> lookup, Collection<String> values, String typeName) {
        Objects.requireNonNull(lookup, "lookup");
        return new SimpleCommandArgumentType<>(kind, displayName, null, values, null, value -> parseRegistryValue(value, lookup, typeName));
    }

    private static <T> CommandArgumentType<T> registryArgument(CommandArgumentKind kind, @Nullable String displayName, KeyedRegistry<T> registry, String typeName) {
        Objects.requireNonNull(registry, "registry");
        return registryArgument(kind, displayName, registry::get, registry.keyStream()
                .map(Identifier::toString)
                .sorted(Comparator.naturalOrder())
                .toList(), typeName);
    }

    private static <T> T parseRegistryValue(String value, Function<Identifier, Optional<T>> lookup, String typeName) {
        Optional<T> result = Objects.requireNonNull(lookup.apply(Identifier.parse(value)), "lookup result");
        return result.orElseThrow(() -> new IllegalArgumentException("Unknown " + typeName + ": " + value));
    }

    private static List<String> identifiers(Collection<Identifier> identifiers) {
        return identifiers.stream()
                .map(Identifier::toString)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static <T> List<String> identifiers(Collection<T> values, Function<T, Identifier> identifier) {
        return values.stream()
                .map(value -> identifier.apply(value).toString())
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static final class SimpleCommandArgumentType<T> implements CommandArgumentType<T> {
        private final CommandArgumentKind kind;
        private final @Nullable String displayName;
        private final @Nullable String enumName;
        private final List<String> values;
        private final @Nullable String postfix;
        private final CommandValueParser<T> valueParser;
        private final @Nullable SuggestionProvider<CommandSourceStack> suggestions;

        private SimpleCommandArgumentType(CommandArgumentKind kind, @Nullable String displayName, @Nullable String enumName, Collection<String> values, @Nullable String postfix) {
            this(kind, displayName, enumName, values, postfix, CommandArgumentTypes::cast, null);
        }

        private SimpleCommandArgumentType(CommandArgumentKind kind, @Nullable String displayName, @Nullable String enumName, Collection<String> values, @Nullable String postfix, CommandValueParser<T> valueParser) {
            this(kind, displayName, enumName, values, postfix, valueParser, null);
        }

        private SimpleCommandArgumentType(CommandArgumentKind kind, @Nullable String displayName, @Nullable String enumName, Collection<String> values, @Nullable String postfix, CommandValueParser<T> valueParser, @Nullable SuggestionProvider<CommandSourceStack> suggestions) {
            this.kind = Objects.requireNonNull(kind, "kind");
            this.displayName = displayName;
            this.enumName = enumName;
            this.values = List.copyOf(Objects.requireNonNull(values, "values"));
            this.postfix = postfix;
            this.valueParser = Objects.requireNonNull(valueParser, "valueParser");
            this.suggestions = suggestions;
            validate();
        }

        @Override
        public CommandArgumentKind getKind() {
            return this.kind;
        }

        @Override
        public @Nullable String getDisplayName() {
            return this.displayName;
        }

        @Override
        public @Nullable String getEnumName() {
            return this.enumName;
        }

        @Override
        public List<String> getValues() {
            return this.values;
        }

        @Override
        public @Nullable String getPostfix() {
            return this.postfix;
        }

        @Override
        public T parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            if (this.kind == CommandArgumentKind.TEXT || this.kind == CommandArgumentKind.MESSAGE
                    || this.kind == CommandArgumentKind.JSON) {
                String value = reader.getRemaining();
                reader.setCursor(reader.getTotalLength());
                return cast(value);
            }

            if (this.kind == CommandArgumentKind.POSITION || this.kind == CommandArgumentKind.BLOCK_POSITION) {
                Coordinate x = readCoordinate(reader);
                reader.skipWhitespace();
                Coordinate y = readCoordinate(reader);
                reader.skipWhitespace();
                Coordinate z = readCoordinate(reader);
                return this.kind == CommandArgumentKind.BLOCK_POSITION
                        ? cast((BlockPositionResolver) source -> resolveBlockPosition(source, x, y, z))
                        : cast((PositionResolver) source -> resolvePosition(source, x, y, z));
            }

            String value = readCommandToken(reader);
            if (value.isEmpty()) {
                reader.setCursor(start);
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, "argument");
            }

            if (this.kind == CommandArgumentKind.FIXED_ENUM && !this.values.contains(value)) {
                reader.setCursor(start);
                throw INVALID_ENUM_VALUE.createWithContext(reader, value);
            }

            if (this.kind == CommandArgumentKind.POSTFIX && !matchesPostfix(value)) {
                reader.setCursor(start);
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, this.postfix);
            }

            try {
                return this.valueParser.apply(value);
            } catch (CommandSyntaxException e) {
                reader.setCursor(start);
                throw e;
            } catch (IllegalArgumentException e) {
                reader.setCursor(start);
                throw INVALID_ENUM_VALUE.createWithContext(reader, value);
            }
        }

        @Override
        public Collection<String> getExamples() {
            if (!this.values.isEmpty()) {
                return this.values.stream().limit(2).toList();
            }

            return List.of("value");
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder suggestions) {
            if (this.suggestions != null && context.getSource() instanceof CommandSourceStack) {
                @SuppressWarnings("unchecked")
                CommandContext<CommandSourceStack> typedContext = (CommandContext<CommandSourceStack>) context;

                try {
                    return this.suggestions.getSuggestions(typedContext, suggestions);
                } catch (CommandSyntaxException e) {
                    return suggestions.buildFuture();
                }
            }

            if (this.values.isEmpty()) {
                return suggestions.buildFuture();
            }

            String remaining = suggestions.getRemainingLowerCase();
            for (String value : this.values) {
                if (value.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    suggestions.suggest(value);
                }
            }

            return suggestions.buildFuture();
        }

        private void validate() {
            switch (this.kind) {
                case FIXED_ENUM -> {
                    if (this.enumName == null || this.enumName.isBlank()) {
                        throw new IllegalArgumentException("Fixed command enums require an enum name");
                    }
                    if (this.values.isEmpty()) {
                        throw new IllegalArgumentException("Fixed command enums require at least one value");
                    }
                }
                case POSTFIX -> {
                    if (this.postfix == null || this.postfix.isBlank()) {
                        throw new IllegalArgumentException("Postfix command arguments require a postfix");
                    }
                }
                default -> {
                }
            }
        }

        private boolean matchesPostfix(String value) {
            String requiredPostfix = Objects.requireNonNull(this.postfix, "postfix");
            return value.length() >= requiredPostfix.length()
                    && value.regionMatches(true, value.length() - requiredPostfix.length(),
                    requiredPostfix, 0, requiredPostfix.length());
        }
    }

    private record NumericCommandArgumentType<T extends Number>(
            CommandArgumentKind kind,
            @Nullable String displayName,
            ArgumentType<T> parser
    ) implements CommandArgumentType<T> {

        private NumericCommandArgumentType {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(parser, "parser");
        }

        @Override
        public CommandArgumentKind getKind() {
            return this.kind;
        }

        @Override
        public @Nullable String getDisplayName() {
            return this.displayName;
        }

        @Override
        public @Nullable String getEnumName() {
            return null;
        }

        @Override
        public List<String> getValues() {
            return List.of();
        }

        @Override
        public @Nullable String getPostfix() {
            return null;
        }

        @Override
        public T parse(StringReader reader) throws CommandSyntaxException {
            return this.parser.parse(reader);
        }

        @Override
        public Collection<String> getExamples() {
            return this.parser.getExamples();
        }
    }

    @FunctionalInterface
    private interface CommandValueParser<T> {
        T apply(String value) throws CommandSyntaxException;
    }

    private record Coordinate(boolean relative, float value) {
        private float resolve(float origin) {
            return this.relative ? origin + this.value : this.value;
        }
    }

    private static Coordinate readCoordinate(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, "value");
        }

        int start = reader.getCursor();
        String value = reader.readUnquotedString();
        try {
            return parseCoordinate(value);
        } catch (IllegalArgumentException e) {
            reader.setCursor(start);
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(reader, value);
        }
    }

    private static RotationResolver parseRotation(String value) throws CommandSyntaxException {
        Coordinate coordinate;
        try {
            coordinate = parseCoordinate(value);
        } catch (IllegalArgumentException e) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().create(value);
        }
        return coordinate::resolve;
    }

    private static Coordinate parseCoordinate(String value) {
        Matcher matcher = RELATIVE_COORDINATE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid coordinate");
        }

        float angle = matcher.group(2) == null ? 0 : Float.parseFloat(matcher.group(2));
        if (!Float.isFinite(angle)) {
            throw new IllegalArgumentException("Coordinate must be finite");
        }
        return new Coordinate(matcher.group(1) != null, angle);
    }

    private static String readCommandToken(StringReader reader) throws CommandSyntaxException {
        if (!reader.canRead()) {
            return "";
        }

        if (StringReader.isQuotedStringStart(reader.peek())) {
            return reader.readQuotedString();
        }

        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }

        return reader.getString().substring(start, reader.getCursor());
    }

    private static Vector3f resolvePosition(CommandSourceStack source, Coordinate x, Coordinate y, Coordinate z) {
        Vector3f origin = source.location().getPosition();
        return Vector3f.from(x.resolve(origin.getX()), y.resolve(origin.getY()), z.resolve(origin.getZ()));
    }

    private static Vector3i resolveBlockPosition(CommandSourceStack source, Coordinate x, Coordinate y, Coordinate z) {
        return resolvePosition(source, x, y, z).floor().toInt();
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }
}
