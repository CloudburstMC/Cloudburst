package org.cloudburstmc.api.block;

import lombok.experimental.UtilityClass;

/**
 * Registry keys for built-in block tags.
 */
@UtilityClass
public class BlockTags {
    public static final BlockTagKey BUTTON = tag("button");
    public static final BlockTagKey CROPS = tag("crops");
    public static final BlockTagKey DOOR = tag("door");
    public static final BlockTagKey DOUBLE_SLAB = tag("double_slab");
    public static final BlockTagKey FENCE = tag("fence");
    public static final BlockTagKey FENCE_GATE = tag("fence_gate");
    public static final BlockTagKey GROUND = tag("ground");
    public static final BlockTagKey LEAVES = tag("leaves");
    public static final BlockTagKey LIQUID = tag("liquid");
    public static final BlockTagKey LOG = tag("log");
    public static final BlockTagKey PLANKS = tag("planks");
    public static final BlockTagKey PRESSURE_PLATE = tag("pressure_plate");
    public static final BlockTagKey RAIL = tag("rail");
    public static final BlockTagKey SAPLING = tag("sapling");
    public static final BlockTagKey SHULKER_BOXES = tag("shulker_boxes");
    public static final BlockTagKey SIGN = tag("sign");
    public static final BlockTagKey SLAB = tag("slab");
    public static final BlockTagKey SOLID = tag("solid");
    public static final BlockTagKey STAIRS = tag("stairs");
    public static final BlockTagKey STEM = tag("stem");
    public static final BlockTagKey TRANSPARENT = tag("transparent");
    public static final BlockTagKey TRAPDOOR = tag("trapdoor");
    public static final BlockTagKey UNSTABLE_BOTTOM_CENTER = tag("unstable_bottom_center");
    public static final BlockTagKey WALLS = tag("walls");
    public static final BlockTagKey WOOL = tag("wool");

    private static BlockTagKey tag(String path) {
        return BlockTagKey.of("minecraft:" + path);
    }
}
