package org.cloudburstmc.server.block.serializer;

import com.google.common.collect.ImmutableMap;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.serializer.util.BedrockStateTags;

@UtilityClass
public class MultiBlockSerializers {

    public static final MultiBlockSerializer STONE_SLAB = new MultiBlockSerializer(ImmutableMap.<String, String>builder()
            .put(BedrockStateTags.TAG_STONE_SLAB_TYPE, "minecraft:stone_slab")
            .put(BedrockStateTags.TAG_STONE_SLAB_TYPE_2, "minecraft:stone_slab2")
            .put(BedrockStateTags.TAG_STONE_SLAB_TYPE_3, "minecraft:stone_slab3")
            .put(BedrockStateTags.TAG_STONE_SLAB_TYPE_4, "minecraft:stone_slab4")
            .build());

    public static final MultiBlockSerializer DOUBLE_STONE_SLAB = new MultiBlockSerializer(ImmutableMap.<String, String>builder()
            .put(BedrockStateTags.TAG_STONE_SLAB_TYPE, "minecraft:double_stone_slab")
            .put(BedrockStateTags.TAG_STONE_SLAB_TYPE_2, "minecraft:double_stone_slab2")
            .put(BedrockStateTags.TAG_STONE_SLAB_TYPE_3, "minecraft:double_stone_slab3")
            .put(BedrockStateTags.TAG_STONE_SLAB_TYPE_4, "minecraft:double_stone_slab4")
            .build());

    public static final MultiBlockSerializer LOG = new MultiBlockSerializer(ImmutableMap.<String, String>builder()
            .put(BedrockStateTags.TAG_OLD_LOG_TYPE, "minecraft:log")
            .put(BedrockStateTags.TAG_NEW_LOG_TYPE, "minecraft:log2")
            .build());

    public static final MultiBlockSerializer LEAVES = new MultiBlockSerializer(ImmutableMap.<String, String>builder()
            .put(BedrockStateTags.TAG_OLD_LEAF_TYPE, "minecraft:leaves")
            .put(BedrockStateTags.TAG_NEW_LEAF_TYPE, "minecraft:leaves2")
            .build());
}
