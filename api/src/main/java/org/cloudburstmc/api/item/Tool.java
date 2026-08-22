package org.cloudburstmc.api.item;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockTagKey;
import org.cloudburstmc.api.block.BlockType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Describes how an item mines blocks.
 *
 * @param rules ordered mining rules; the first matching rule that defines a queried property is used
 * @param defaultMiningSpeed speed used when no matching rule supplies one
 * @param damagePerBlock durability consumed for each mined block
 * @param canDestroyBlocksInCreative whether the item can destroy blocks in creative mode
 */
public record Tool(List<Rule> rules, float defaultMiningSpeed, int damagePerBlock, boolean canDestroyBlocksInCreative) {

    public Tool {
        checkArgument(defaultMiningSpeed > 0, "defaultMiningSpeed must be positive");
        checkArgument(damagePerBlock >= 0, "damagePerBlock cannot be negative");
        rules = List.copyOf(checkNotNull(rules, "rules"));
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the mining speed for a block type.
     */
    public float getMiningSpeed(BlockType blockType) {
        checkNotNull(blockType, "blockType");
        for (Rule rule : this.rules) {
            if (rule.speed() != null && rule.matches(blockType)) {
                return rule.speed();
            }
        }

        return this.defaultMiningSpeed;
    }

    /**
     * Returns whether this tool is correct for drops from a block type.
     */
    public boolean isCorrectForDrops(BlockType blockType) {
        checkNotNull(blockType, "blockType");
        for (Rule rule : this.rules) {
            if (rule.correctForDrops() != null && rule.matches(blockType)) {
                return rule.correctForDrops();
            }
        }

        return false;
    }

    /**
     * A mining rule applied to explicit block types or members of block tags.
     *
     * @param blocks block types matched by this rule
     * @param tags block tags matched by this rule
     * @param speed matching mining speed, or {@code null} to use the tool default
     * @param correctForDrops whether this rule makes the tool correct for drops, or {@code null} not to override it
     */
    public record Rule(Set<BlockType> blocks, Set<BlockTagKey> tags, @Nullable Float speed, @Nullable Boolean correctForDrops) {

        public Rule {
            blocks = Set.copyOf(checkNotNull(blocks, "blocks"));
            tags = Set.copyOf(checkNotNull(tags, "tags"));
            checkArgument(!blocks.isEmpty() || !tags.isEmpty(), "A tool rule must match at least one block or tag");
            checkArgument(speed == null || speed > 0, "speed must be positive");
        }

        /**
         * Creates a rule matching explicit block types.
         */
        public static Rule blocks(Collection<BlockType> blocks, @Nullable Float speed, @Nullable Boolean correctForDrops) {
            return new Rule(Set.copyOf(blocks), Set.of(), speed, correctForDrops);
        }

        /**
         * Creates a rule matching members of a block tag.
         */
        public static Rule tag(BlockTagKey tag, @Nullable Float speed, @Nullable Boolean correctForDrops) {
            return new Rule(Set.of(), Set.of(checkNotNull(tag, "tag")), speed, correctForDrops);
        }

        /**
         * Creates a rule that mines matching blocks at the supplied speed and marks the tool correct for drops.
         */
        public static Rule minesAndDrops(Collection<BlockType> blocks, float speed) {
            return blocks(blocks, speed, true);
        }

        /**
         * Creates a rule that mines matching blocks at the supplied speed and marks the tool correct for drops.
         */
        public static Rule minesAndDrops(BlockTagKey tag, float speed) {
            return tag(tag, speed, true);
        }

        /**
         * Creates a rule that denies drops for matching blocks.
         */
        public static Rule deniesDrops(Collection<BlockType> blocks) {
            return blocks(blocks, null, false);
        }

        /**
         * Creates a rule that denies drops for matching blocks.
         */
        public static Rule deniesDrops(BlockTagKey tag) {
            return tag(tag, null, false);
        }

        /**
         * Creates a rule that only overrides the mining speed for matching blocks.
         */
        public static Rule overrideSpeed(Collection<BlockType> blocks, float speed) {
            return blocks(blocks, speed, null);
        }

        /**
         * Creates a rule that only overrides the mining speed for matching blocks.
         */
        public static Rule overrideSpeed(BlockTagKey tag, float speed) {
            return tag(tag, speed, null);
        }

        /**
         * Tests whether this rule applies to a block type.
         */
        public boolean matches(BlockType blockType) {
            checkNotNull(blockType, "blockType");
            if (this.blocks.contains(blockType)) {
                return true;
            }

            for (BlockTagKey tag : this.tags) {
                if (blockType.is(tag)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Builds immutable tool behavior.
     */
    public static final class Builder {
        private final List<Rule> rules = new ArrayList<>();
        private float defaultMiningSpeed = 1;
        private int damagePerBlock = 1;
        private boolean canDestroyBlocksInCreative = true;

        private Builder() {
        }

        /**
         * Sets the mining speed used when no rule overrides it.
         */
        public Builder defaultMiningSpeed(float defaultMiningSpeed) {
            this.defaultMiningSpeed = defaultMiningSpeed;
            return this;
        }

        /**
         * Sets the durability consumed for each mined block.
         */
        public Builder damagePerBlock(int damagePerBlock) {
            this.damagePerBlock = damagePerBlock;
            return this;
        }

        /**
         * Appends a mining rule.
         */
        public Builder addRule(Rule rule) {
            this.rules.add(checkNotNull(rule, "rule"));
            return this;
        }

        /**
         * Appends mining rules in iteration order.
         */
        public Builder addRules(Collection<Rule> rules) {
            checkNotNull(rules, "rules").forEach(this::addRule);
            return this;
        }

        /**
         * Sets whether this tool can destroy blocks in creative mode.
         */
        public Builder canDestroyBlocksInCreative(boolean canDestroyBlocksInCreative) {
            this.canDestroyBlocksInCreative = canDestroyBlocksInCreative;
            return this;
        }

        /**
         * Creates the immutable tool behavior.
         */
        public Tool build() {
            return new Tool(this.rules, this.defaultMiningSpeed, this.damagePerBlock, this.canDestroyBlocksInCreative);
        }
    }
}
