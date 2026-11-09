package org.cloudburstmc.api.util.data;

/**
 * How a redstone comparator combines its rear and side inputs.
 */
public enum ComparatorMode {
    /**
     * Retains the rear signal when neither side input exceeds it.
     */
    COMPARE,
    /**
     * Subtracts the greater side input from the rear signal, with a minimum of zero.
     */
    SUBTRACT
}
