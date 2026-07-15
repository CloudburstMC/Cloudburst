package org.cloudburstmc.api.block;

import java.util.Set;

/**
 * A named, read-only set of block types.
 */
public interface BlockTag {

    /**
     * @return the key used to identify this tag
     */
    BlockTagKey getKey();

    /**
     * @param type block type to test
     * @return whether {@code type} is a member
     */
    boolean isTagged(BlockType type);

    /**
     * @return all members in an immutable set
     */
    Set<BlockType> getValues();
}
