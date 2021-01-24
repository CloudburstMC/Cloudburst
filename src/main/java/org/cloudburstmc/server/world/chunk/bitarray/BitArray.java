package org.cloudburstmc.server.world.chunk.bitarray;

public interface BitArray {

    void set(int index, int value);

    int get(int index);

    int size();

    int[] getWords();

    BitArrayVersion getVersion();

    BitArray copy();
}
