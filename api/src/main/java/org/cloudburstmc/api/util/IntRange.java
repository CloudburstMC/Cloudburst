package org.cloudburstmc.api.util;

import com.google.common.base.Preconditions;

import java.util.AbstractList;
import java.util.RandomAccess;

/**
 * An immutable list of consecutive integers between inclusive bounds.
 */
public class IntRange extends AbstractList<Integer> implements RandomAccess {

    private final int start;
    private final int end;
    private final int size;

    /**
     * @param start the inclusive minimum
     * @param end   the inclusive maximum
     * @throws IllegalArgumentException if the range is empty or its size exceeds list capacity
     */
    public IntRange(int start, int end) {
        long length = (long) end - start + 1;
        Preconditions.checkArgument(length > 0 && length <= Integer.MAX_VALUE, "Range size must fit a nonempty list");
        this.start = start;
        this.end = end;
        this.size = (int) length;
    }

    /**
     * @return the inclusive minimum
     */
    public int getStart() {
        return start;
    }

    /**
     * @return the inclusive maximum
     */
    public int getEnd() {
        return end;
    }

    @Override
    public Integer get(int index) {
        return this.start + Preconditions.checkElementIndex(index, this.size);
    }

    /**
     * @param index the zero-based position
     * @return the value without boxing
     * @throws IndexOutOfBoundsException if the index is outside the list
     */
    public int getInt(int index) {
        return this.start + Preconditions.checkElementIndex(index, this.size);
    }

    @Override
    public int size() {
        return size;
    }
}
