package org.cloudburstmc.server.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.command.CommandPermissions;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.vector.Vector3f;

import java.util.*;

/**
 * Parsed target selector that can be resolved against a command source.
 *
 * <p>The selector may represent a literal name or one of the supported selector forms
 * such as {@code @p}, {@code @a}, {@code @s}, {@code @e}, {@code @r}, or {@code @n}.
 * Optional filters narrow the candidate set before sorting and limiting are applied.</p>
 *
 * @param input            original selector input
 * @param kind             selector root kind
 * @param playersOnly      whether this selector is restricted to players
 * @param maxResults       maximum number of resolved targets
 * @param position         optional origin override
 * @param box              optional volume filter
 * @param minDistance      minimum distance from the selector origin, or {@code null}
 * @param maxDistance      maximum distance from the selector origin, or {@code null}
 * @param name             optional name filter
 * @param invertedName     whether the name filter is inverted
 * @param type             optional entity type filter
 * @param invertedType     whether the type filter is inverted
 * @param gameMode         optional player game mode filter
 * @param invertedGameMode whether the game mode filter is inverted
 * @param minLevel         minimum player experience level, or {@code null}
 * @param maxLevel         maximum player experience level, or {@code null}
 * @param minPitch         minimum pitch, or {@code null}
 * @param maxPitch         maximum pitch, or {@code null}
 * @param minYaw           minimum yaw, or {@code null}
 * @param maxYaw           maximum yaw, or {@code null}
 * @param tags             required and excluded command tags
 * @param sort             match ordering before limiting
 */
public record CloudTargetSelector(String input, CloudSelectorKind kind, boolean playersOnly, int maxResults,
                             @Nullable CloudSelectorPosition position, @Nullable CloudSelectorBox box,
                             @Nullable Float minDistance, @Nullable Float maxDistance,
                             @Nullable String name, boolean invertedName,
                             @Nullable Identifier type, boolean invertedType,
                             @Nullable GameMode gameMode, boolean invertedGameMode,
                             @Nullable Integer minLevel, @Nullable Integer maxLevel,
                             @Nullable Float minPitch, @Nullable Float maxPitch,
                             @Nullable Float minYaw, @Nullable Float maxYaw,
                             List<CloudSelectorTag> tags,
                             CloudSelectorSort sort) {
    private static final DynamicCommandExceptionType ERROR_UNSUPPORTED_PLAYER_SELECTOR =
            new DynamicCommandExceptionType(value -> new LiteralMessage("Unsupported player selector " + value));
    private static final DynamicCommandExceptionType ERROR_SELECTOR_NOT_ALLOWED =
            new DynamicCommandExceptionType(value -> new LiteralMessage("Target selectors are not allowed: " + value));

    public CloudTargetSelector {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(kind, "kind");
        tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
        Objects.requireNonNull(sort, "sort");
    }

    /**
     * Resolves this selector to players.
     *
     * @param source command source used for executor, location, level, and online players
     * @return matching players
     * @throws CommandSyntaxException if the source cannot use selectors or this selector cannot be used where only
     *                                players are accepted
     */
    public List<Player> resolvePlayers(CommandSourceStack source) throws CommandSyntaxException {
        this.checkPermission(source);
        if (!this.playersOnly && this.kind != CloudSelectorKind.SELF) {
            throw ERROR_UNSUPPORTED_PLAYER_SELECTOR.create(this.input);
        }

        List<Player> players;
        if (this.kind == CloudSelectorKind.NAME) {
            Player player = source.sender().getServer().getPlayer(Objects.requireNonNull(this.name, "name"));
            players = player == null ? List.of() : List.of(player);
        } else if (this.kind == CloudSelectorKind.SELF) {
            Entity executor = source.executor();
            players = executor instanceof Player player ? List.of(player) : List.of();
        } else {
            players = new ArrayList<>(source.sender().getServer().getOnlinePlayers().values());
        }

        Vector3f origin = this.origin(source);
        Level level = source.location().getLevel();
        return this.sortAndLimit(players.stream()
                .filter(player -> this.matches(level, origin, player))
                .toList(), origin);
    }

    /**
     * Resolves this selector to entities.
     *
     * @param source command source used for executor, location, levels, and online players
     * @return matching entities
     * @throws CommandSyntaxException if the source cannot use selectors or resolving the executor selector requires
     *                                an entity executor
     */
    public List<Entity> resolveEntities(CommandSourceStack source) throws CommandSyntaxException {
        this.checkPermission(source);
        List<Entity> entities;
        if (this.kind == CloudSelectorKind.NAME) {
            entities = this.resolveNamedEntity(source);
        } else if (this.kind == CloudSelectorKind.SELF) {
            entities = List.of(source.entityOrThrow());
        } else if (this.playersOnly) {
            entities = source.sender().getServer().getOnlinePlayers().values().stream()
                    .map(Entity.class::cast)
                    .toList();
        } else {
            entities = source.location().getLevel().getEntities().stream()
                    .map(Entity.class::cast)
                    .toList();
        }

        Vector3f origin = this.origin(source);
        Level level = source.location().getLevel();
        return this.sortAndLimit(entities.stream()
                .filter(Entity::isAlive)
                .filter(entity -> this.matches(level, origin, entity))
                .toList(), origin);
    }

    private void checkPermission(CommandSourceStack source) throws CommandSyntaxException {
        Objects.requireNonNull(source, "source");
        if (this.kind != CloudSelectorKind.NAME
                && !source.sender().hasPermission(CommandPermissions.TARGET_SELECTORS)) {
            throw ERROR_SELECTOR_NOT_ALLOWED.create(this.input);
        }
    }

    private List<Entity> resolveNamedEntity(CommandSourceStack source) {
        String entityName = Objects.requireNonNull(this.name, "name");
        Player player = source.sender().getServer().getPlayer(entityName);
        if (player != null) {
            return List.of(player);
        }

        return source.sender().getServer().getLevels().stream()
                .map(Level::getEntities)
                .flatMap(Collection::stream)
                .filter(entity -> entity.getName().equalsIgnoreCase(entityName))
                .map(Entity.class::cast)
                .findFirst()
                .map(List::of)
                .orElse(List.of());
    }

    private boolean matches(Level level, Vector3f origin, Entity entity) {
        if (this.kind == CloudSelectorKind.NAME) {
            return true;
        }

        if ((this.position != null || this.box != null || this.minDistance != null || this.maxDistance != null)
                && entity.getLevel() != level) {
            return false;
        }

        if (this.box != null && !this.box.absolute(origin).intersects(entity.getBoundingBox())) {
            return false;
        }

        double distance = distanceSquared(origin, entity);
        if (this.minDistance != null && distance < this.minDistance * this.minDistance) {
            return false;
        }

        if (this.maxDistance != null && distance > this.maxDistance * this.maxDistance) {
            return false;
        }

        if (this.name != null && (entity.getName().equalsIgnoreCase(this.name) == this.invertedName)) {
            return false;
        }

        if (this.type != null && (entity.getType().getId().equals(this.type) == this.invertedType)) {
            return false;
        }

        for (CloudSelectorTag tag : this.tags) {
            boolean matches = tag.value().isEmpty()
                    ? entity.getScoreboardTags().isEmpty()
                    : entity.hasScoreboardTag(tag.value());
            if (matches == tag.inverted()) {
                return false;
            }
        }

        if (this.gameMode != null && entity instanceof Player player && (player.getGameMode() == this.gameMode) == this.invertedGameMode) {
            return false;
        }

        if ((this.gameMode != null || this.minLevel != null || this.maxLevel != null) && !(entity instanceof Player)) {
            return false;
        }

        if (entity instanceof Player player) {
            if (this.minLevel != null && player.getExperienceLevel() < this.minLevel) {
                return false;
            }
            if (this.maxLevel != null && player.getExperienceLevel() > this.maxLevel) {
                return false;
            }
        }

        float pitch = entity.getPitch();
        if ((this.minPitch != null && pitch < this.minPitch)
                || (this.maxPitch != null && pitch > this.maxPitch)) {
            return false;
        }

        float yaw = normalizeYaw(entity.getYaw());
        return (this.minYaw == null || yaw >= this.minYaw) && (this.maxYaw == null || yaw <= this.maxYaw);
    }

    private Vector3f origin(CommandSourceStack source) {
        Vector3f sourcePosition = source.location().getPosition();
        return this.position == null ? sourcePosition : this.position.resolve(sourcePosition);
    }

    private <T extends Entity> List<T> sortAndLimit(List<T> entities, Vector3f origin) {
        List<T> result = new ArrayList<>(entities);
        switch (this.sort) {
            case NEAREST -> result.sort(Comparator.comparingDouble(entity -> distanceSquared(origin, entity)));
            case FURTHEST -> result.sort(Comparator.comparingDouble((Entity entity) -> distanceSquared(origin, entity)).reversed());
            case RANDOM -> Collections.shuffle(result);
            case ARBITRARY -> {
            }
        }
        return List.copyOf(result.subList(0, Math.min(this.maxResults, result.size())));
    }

    private static double distanceSquared(Vector3f position, Entity entity) {
        double x = entity.getX() - position.getX();
        double y = entity.getY() - position.getY();
        double z = entity.getZ() - position.getZ();
        return x * x + y * y + z * z;
    }

    private static float normalizeYaw(float yaw) {
        float normalized = (yaw + 90.0f) % 360.0f;
        if (normalized < 0.0f) {
            normalized += 360.0f;
        }
        return normalized - 180.0f;
    }
}
