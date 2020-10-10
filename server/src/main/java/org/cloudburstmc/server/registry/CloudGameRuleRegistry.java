package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.cloudburstmc.api.level.gamerule.*;
import org.cloudburstmc.api.registry.GameRuleRegistry;
import org.cloudburstmc.server.level.gamerule.GameRuleMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CloudGameRuleRegistry implements GameRuleRegistry, Registry {
    private static final CloudGameRuleRegistry INSTANCE = new CloudGameRuleRegistry();

    private final Map<String, GameRule<?>> registered = new HashMap<>();
    private volatile boolean closed;

    private CloudGameRuleRegistry() {
        this.registerVanilla();
    }

    public static CloudGameRuleRegistry get() {
        return INSTANCE;
    }

    @Override
    public <T extends Comparable<T>> void register(GameRule<T> gameRule) {
        Preconditions.checkState(!closed, "Registry has closed");
        Preconditions.checkNotNull(gameRule, "gameRule");
        Preconditions.checkArgument(gameRule instanceof BooleanGameRule ||
                        gameRule instanceof IntegerGameRule || gameRule instanceof FloatGameRule,
                "Invalid gamerule type given");
        Preconditions.checkArgument(this.registered.putIfAbsent(gameRule.getName().toLowerCase(), gameRule) == null,
                "GameRule already registered");
    }

    @Nullable
    @Override
    public GameRule<? extends Comparable<?>> fromString(String name) {
        name = name.trim().toLowerCase();
        return this.registered.get(name);
    }

    @Nonnull
    @SuppressWarnings({"unchecked", "rawtypes"})
    public GameRuleMap getDefaultRules() {
        GameRuleMap gameRules = new GameRuleMap();
        for (GameRule gameRule : this.registered.values()) {
            gameRules.put(gameRule, gameRule.getDefaultValue());
        }
        return gameRules;
    }

    @Nonnull
    @Override
    public List<GameRule<?>> getRules() {
        return ImmutableList.copyOf(this.registered.values());
    }

    @Nonnull
    @Override
    public Set<String> getRuleNames() {
        return ImmutableSet.copyOf(this.registered.keySet());
    }

    @Override
    public void close() {
        this.closed = true;
    }

    private void registerVanilla() {
        this.register(GameRules.COMMAND_BLOCK_OUTPUT);
        this.register(GameRules.DO_DAYLIGHT_CYCLE);
        this.register(GameRules.DO_ENTITY_DROPS);
        this.register(GameRules.DO_FIRE_TICK);
        this.register(GameRules.DO_MOB_LOOT);
        this.register(GameRules.DO_MOB_SPAWNING);
        this.register(GameRules.DO_TILE_DROPS);
        this.register(GameRules.DO_WEATHER_CYCLE);
        this.register(GameRules.DROWNING_DAMAGE);
        this.register(GameRules.FALL_DAMAGE);
        this.register(GameRules.FIRE_DAMAGE);
        this.register(GameRules.KEEP_INVENTORY);
        this.register(GameRules.MOB_GRIEFING);
        this.register(GameRules.PVP);
        this.register(GameRules.SHOW_COORDINATES);
        this.register(GameRules.NATURAL_REGENERATION);
        this.register(GameRules.TNT_EXPLODES);
        this.register(GameRules.SEND_COMMAND_FEEDBACK);
        this.register(GameRules.EXPERIMENTAL_GAME_PLAY);
        this.register(GameRules.MAX_COMMAND_CHAIN_LENGTH);
        this.register(GameRules.DO_INSOMNIA);
        this.register(GameRules.COMMAND_BLOCKS_ENABLED);
        this.register(GameRules.RANDOM_TICK_SPEED);
        this.register(GameRules.DO_IMMEDIATE_RESPAWN);
        this.register(GameRules.SHOW_DEATH_MESSAGES);
        this.register(GameRules.FUNCTION_COMMAND_LIMIT);
        this.register(GameRules.SPAWN_RADIUS);
    }
}
