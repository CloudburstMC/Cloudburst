package org.cloudburstmc.server.item.loot;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class WeightedTable<T> {

    private final List<Entry<T>> entries;
    private final double totalWeight;

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public Optional<T> select(Random random) {
        Objects.requireNonNull(random, "random");
        if (this.entries.isEmpty()) {
            return Optional.empty();
        }

        double selectedWeight = random.nextDouble(this.totalWeight);
        double accumulatedWeight = 0;
        for (Entry<T> entry : this.entries) {
            accumulatedWeight += entry.weight();
            if (accumulatedWeight > selectedWeight) {
                return Optional.of(entry.value());
            }
        }

        return Optional.of(this.entries.getLast().value());
    }

    private record Entry<T>(T value, double weight) {

        private Entry {
            if (value == null) {
                throw new IllegalArgumentException("Weighted table values cannot be null");
            }
            if (!Double.isFinite(weight) || weight <= 0) {
                throw new IllegalArgumentException("Weighted table weights must be positive finite values");
            }
        }
    }

    public static final class Builder<T> {

        private final List<Entry<T>> entries = new ArrayList<>();
        private double totalWeight;

        public Builder<T> add(T value, double weight) {
            Entry<T> entry = new Entry<>(value, weight);
            this.entries.add(entry);
            this.totalWeight += weight;
            return this;
        }

        public WeightedTable<T> build() {
            if (!Double.isFinite(this.totalWeight)) {
                throw new IllegalStateException("Weighted table total weight must be finite");
            }
            return new WeightedTable<>(List.copyOf(this.entries), this.totalWeight);
        }
    }
}
