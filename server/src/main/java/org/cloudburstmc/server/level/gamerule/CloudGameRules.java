package org.cloudburstmc.server.level.gamerule;

import lombok.Getter;
import org.cloudburstmc.api.level.gamerule.GameRule;
import org.cloudburstmc.api.level.gamerule.LevelGameRules;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class CloudGameRules implements LevelGameRules {
    private final Map<GameRule<?>, Comparable<?>> values = new LinkedHashMap<>();

    @Getter
    private volatile boolean dirty;

    public CloudGameRules() {
    }

    public CloudGameRules(LevelGameRules gameRules) {
        this.loadFrom(gameRules);
    }

    @Override
    public boolean contains(GameRule<?> gameRule) {
        Objects.requireNonNull(gameRule, "gameRule");
        return this.values.containsKey(gameRule);
    }

    @Override
    public <T extends Comparable<T>> T get(GameRule<T> gameRule) {
        return this.find(gameRule).orElseThrow(() -> new NoSuchElementException(
                "No value has been registered for game rule " + gameRule.getName()));
    }

    @Override
    public <T extends Comparable<T>> Optional<T> find(GameRule<T> gameRule) {
        Objects.requireNonNull(gameRule, "gameRule");
        Comparable<?> value = this.values.get(gameRule);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(this.cast(gameRule, value));
    }

    @Override
    public <T extends Comparable<T>> boolean set(GameRule<T> gameRule, T value) {
        return this.setValue(gameRule, value, true);
    }

    public <T extends Comparable<T>> void load(GameRule<T> gameRule, T value) {
        this.setValue(gameRule, value, false);
    }

    public void loadFrom(LevelGameRules gameRules) {
        Objects.requireNonNull(gameRules, "gameRules");
        if (gameRules == this) {
            this.markClean();
            return;
        }

        this.values.clear();
        for (Entry<?> entry : gameRules) {
            this.loadEntry(entry);
        }

        this.markClean();
    }

    public void markClean() {
        this.dirty = false;
    }

    @Override
    public Collection<GameRule<?>> rules() {
        return List.copyOf(this.values.keySet());
    }

    @Override
    public int size() {
        return this.values.size();
    }

    @Override
    public @NonNull Iterator<Entry<?>> iterator() {
        ArrayList<Entry<?>> entries = new ArrayList<>(this.values.size());
        this.values.forEach((gameRule, value) -> entries.add(this.entry(gameRule, value)));
        return Collections.unmodifiableList(entries).iterator();
    }

    @Override
    public String toString() {
        return this.values.toString();
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof CloudGameRules that)) {
            return false;
        }

        return this.values.equals(that.values);
    }

    private <T extends Comparable<T>> boolean setValue(GameRule<T> gameRule, T value, boolean markDirty) {
        Objects.requireNonNull(gameRule, "gameRule");
        Objects.requireNonNull(value, "value");
        if (!gameRule.getValueClass().isInstance(value)) {
            throw new IllegalArgumentException("Value for " + gameRule.getName() + " must be a " + gameRule.getValueClass().getSimpleName());
        }

        Comparable<?> oldValue = this.values.put(gameRule, value);
        boolean changed = !value.equals(oldValue);
        if (changed && markDirty) {
            this.dirty = true;
        }

        return changed;
    }

    private <T extends Comparable<T>> void loadEntry(Entry<T> entry) {
        this.load(entry.rule(), entry.value());
    }

    private <T extends Comparable<T>> Entry<T> entry(GameRule<T> gameRule, Comparable<?> value) {
        return new Entry<>(gameRule, this.cast(gameRule, value));
    }

    private <T extends Comparable<T>> T cast(GameRule<T> gameRule, Comparable<?> value) {
        return gameRule.getValueClass().cast(value);
    }
}
