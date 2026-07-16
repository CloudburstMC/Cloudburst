package org.cloudburstmc.api.block;

public interface BlockSnapshot {

    default BlockState getState() {
        return getState(0);
    }

    default BlockState getExtra() {
        return getState(1);
    }

    BlockState getState(int layer);

    /**
     * @return the layer containing liquid, or {@code -1} when neither layer contains liquid
     */
    default int getLiquidLayer() {
        if (getState().getType().isLiquid()) {
            return 0;
        }
        return getExtra().getType().isLiquid() ? 1 : -1;
    }

    /**
     * @return the liquid in either layer, or the empty liquid state when none is present
     */
    default LiquidState getLiquid() {
        return switch (getLiquidLayer()) {
            case 0 -> LiquidState.of(getState());
            case 1 -> LiquidState.of(getExtra());
            default -> LiquidState.empty();
        };
    }

    /**
     * @return whether either block layer contains liquid
     */
    default boolean containsLiquid() {
        return getLiquidLayer() >= 0;
    }
}
