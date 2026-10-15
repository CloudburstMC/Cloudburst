package org.cloudburstmc.server.container.cartography;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.map.MapScale;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CartographyResultCalculator {

    private static final List<EmptyMapRecipe> EMPTY_MAP_RECIPES = List.of(
            new EmptyMapRecipe(ItemTypes.PAPER, null, false),
            new EmptyMapRecipe(ItemTypes.PAPER, ItemTypes.COMPASS, true),
            new EmptyMapRecipe(ItemTypes.EMPTY_MAP, ItemTypes.COMPASS, true)
    );

    public static Result calculate(ItemStack input, ItemStack additional, @Nullable MapState map, @Nullable String renameText) {
        Result result = calculateOrdered(input, additional, map, renameText);
        if (!result.item().isEmpty()) {
            return result;
        }

        Result reversed = calculateOrdered(additional, input, map, renameText);
        return reversed.item().isEmpty() ? Result.EMPTY : new Result(reversed.item(), reversed.operation(), reversed.additionalCount(), reversed.inputCount(), reversed.map());
    }

    private static Result calculateOrdered(ItemStack input, ItemStack additional, @Nullable MapState map, @Nullable String renameText) {
        if (input.isEmpty()) {
            return Result.EMPTY;
        }

        Result result = Result.EMPTY;
        for (EmptyMapRecipe recipe : EMPTY_MAP_RECIPES) {
            if (input.getType() != recipe.input() || (recipe.additional() == null ? !additional.isEmpty() : additional.getType() != recipe.additional())) {
                continue;
            }

            if (input.getType() == ItemTypes.EMPTY_MAP && input.getOrDefault(ItemDataComponents.MAP_TRACKING_POSITION, false)) {
                return Result.EMPTY;
            }

            ItemStack item = input.getType() == ItemTypes.EMPTY_MAP ? input.withCount(1) : ItemStack.builder(ItemTypes.EMPTY_MAP).build();
            item = item.toBuilder().setData(ItemDataComponents.MAP_TRACKING_POSITION, recipe.tracking()).build();
            result = new Result(item, Operation.CREATE_EMPTY, 1, additional.isEmpty() ? 0 : 1, null);
            break;
        }

        if (input.getType() == ItemTypes.FILLED_MAP && map != null) {
            ItemStack item = input.withCount(1).toBuilder().setData(ItemDataComponents.MAP_TRACKING_POSITION, map.tracking()).build();
            if (additional.getType() == ItemTypes.EMPTY_MAP) {
                result = new Result(item.withCount(2), Operation.CLONE, 1, 1, map);
            } else if (additional.getType() == ItemTypes.PAPER && !map.locked() && map.scale() != MapScale.FARTHEST) {
                result = new Result(item, Operation.SCALE, 1, 1, map);
            } else if (additional.getType() == ItemTypes.GLASS_PANE && !map.locked()) {
                result = new Result(item, Operation.LOCK, 1, 1, map);
            } else if (additional.getType() == ItemTypes.COMPASS && !map.tracking()) {
                item = item.toBuilder().setData(ItemDataComponents.MAP_TRACKING_POSITION, true).build();
                result = new Result(item, Operation.ADD_LOCATOR, 1, 1, map);
            }
        }

        String name = renameText == null || renameText.isEmpty() ? null : renameText;
        if (result.item().isEmpty() && additional.isEmpty() && (input.getType() == ItemTypes.FILLED_MAP || input.getType() == ItemTypes.EMPTY_MAP)) {
            result = new Result(input, Operation.RENAME, 1, 0, null);
        }

        if (result.item().isEmpty() || renameText == null) {
            return result;
        }

        ItemStack renamed = name == null ? result.item().toBuilder().removeData(ItemDataComponents.CUSTOM_NAME).build()
                : result.item().toBuilder().setData(ItemDataComponents.CUSTOM_NAME, name).build();
        return new Result(renamed, result.operation(), result.inputCount(), result.additionalCount(), result.map());
    }

    private record EmptyMapRecipe(ItemType input, @Nullable ItemType additional, boolean tracking) {
    }

    public record MapState(MapScale scale, boolean locked, boolean tracking) {
    }

    public record Result(ItemStack item, Operation operation, int inputCount, int additionalCount, @Nullable MapState map) {

        public static final Result EMPTY = new Result(ItemStack.EMPTY, Operation.NONE, 0, 0, null);

        public boolean canMerge() {
            return this.operation == Operation.CREATE_EMPTY || this.operation == Operation.CLONE
                    || this.operation == Operation.RENAME || this.operation == Operation.CUSTOM;
        }

        public Result prepare(ItemStack output, ItemStack input, ItemStack additional) {
            if (this.operation != Operation.NONE || output.isEmpty()) {
                return this;
            }

            return new Result(output, Operation.CUSTOM, input.isEmpty() ? 0 : 1, additional.isEmpty() ? 0 : 1, null);
        }

        public boolean createsMapCopy() {
            return this.operation == Operation.SCALE || this.operation == Operation.LOCK || this.operation == Operation.ADD_LOCATOR;
        }

        public @Nullable MapState outputMap() {
            if (this.map == null) {
                return null;
            }

            MapScale scale = this.map.scale();
            if (this.operation == Operation.SCALE) {
                scale = switch (scale) {
                    case CLOSEST -> MapScale.CLOSE;
                    case CLOSE -> MapScale.NORMAL;
                    case NORMAL -> MapScale.FAR;
                    case FAR -> MapScale.FARTHEST;
                    case FARTHEST -> throw new IllegalStateException("Cannot expand a maximum-scale map");
                };
            }

            return new MapState(scale, this.map.locked() || this.operation == Operation.LOCK, this.map.tracking() || this.operation == Operation.ADD_LOCATOR);
        }

        public int consumedInput(int count, int outputCount) {
            return this.inputCount * (this.operation == Operation.RENAME ? count : count / outputCount);
        }

        public int consumedAdditional(int count, int outputCount) {
            return this.additionalCount * (this.operation == Operation.RENAME ? count : count / outputCount);
        }
    }

    public enum Operation {
        NONE,
        CUSTOM,
        CREATE_EMPTY,
        CLONE,
        SCALE,
        LOCK,
        ADD_LOCATOR,
        RENAME
    }
}
