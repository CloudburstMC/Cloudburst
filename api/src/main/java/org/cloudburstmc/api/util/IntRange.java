package org.cloudburstmc.api.util;

import com.google.common.base.Preconditions;

import java.util.AbstractList;

public class IntRange extends AbstractList<Integer> {
    private final int start;
    private final int end;
    private final int size;

    public IntRange(int start, int end) {
        Preconditions.checkArgument(start < end, "Start is larger than end");
        this.start = start;
        this.end = end;
        this.size = (end - start) + 1;
    }


    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public Integer get(int index) {
        return this.start + Preconditions.checkElementIndex(index, this.size);
    }

    public int getInt(int index) {
        return this.start + Preconditions.checkElementIndex(index, this.size);
    }

    @Override
    public int size() {
        return size;
    }
}
