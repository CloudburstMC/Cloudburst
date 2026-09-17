package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.data.ItemLockMode;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.Bootstrap;
import tools.jackson.databind.JsonNode;

import java.util.*;
import java.util.function.Function;

public class ItemCommandComponents {
    private static final String CAN_DESTROY = "minecraft:can_destroy";
    private static final String CAN_PLACE_ON = "minecraft:can_place_on";
    private static final String ITEM_LOCK = "minecraft:item_lock";
    private static final String KEEP_ON_DEATH = "minecraft:keep_on_death";
    private static final Set<String> SUPPORTED_COMPONENTS = Set.of(
            CAN_DESTROY,
            CAN_PLACE_ON,
            ITEM_LOCK,
            KEEP_ON_DEATH
    );

    private final List<BlockType> canDestroy;
    private final List<BlockType> canPlaceOn;
    private final ItemLockMode itemLock;
    private final boolean keepOnDeath;

    private ItemCommandComponents(List<BlockType> canDestroy, List<BlockType> canPlaceOn, ItemLockMode itemLock, boolean keepOnDeath) {
        this.canDestroy = canDestroy;
        this.canPlaceOn = canPlaceOn;
        this.itemLock = itemLock;
        this.keepOnDeath = keepOnDeath;
    }

    /**
     * Parses and validates an item component object.
     *
     * @param input         serialized component object
     * @param blockResolver resolver for block identifiers used by placement and destruction restrictions
     * @return parsed components
     * @throws IllegalArgumentException if the object is malformed or contains an unsupported value
     */
    public static ItemCommandComponents parse(String input, Function<Identifier, Optional<BlockType>> blockResolver) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(blockResolver, "blockResolver");

        JsonNode root;
        try {
            root = Bootstrap.JSON_MAPPER.readTree(input);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid item components JSON", e);
        }

        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Item components must be a JSON object");
        }

        for (Map.Entry<String, JsonNode> property : root.properties()) {
            if (!SUPPORTED_COMPONENTS.contains(property.getKey())) {
                throw new IllegalArgumentException("Unknown item component '" + property.getKey() + "'");
            }
        }

        return new ItemCommandComponents(
                readBlocks(root.get(CAN_DESTROY), CAN_DESTROY, blockResolver),
                readBlocks(root.get(CAN_PLACE_ON), CAN_PLACE_ON, blockResolver),
                readItemLock(root.get(ITEM_LOCK)),
                readKeepOnDeath(root.get(KEEP_ON_DEATH))
        );
    }

    private static List<BlockType> readBlocks(JsonNode component, String name, Function<Identifier, Optional<BlockType>> blockResolver) {
        if (component == null) {
            return null;
        }

        if (!component.isObject()) {
            throw new IllegalArgumentException(name + " must be an object");
        }

        JsonNode blocks = component.get("blocks");
        if (blocks == null || !blocks.isArray()) {
            throw new IllegalArgumentException(name + ".blocks must be an array");
        }

        List<BlockType> result = new ArrayList<>();
        for (JsonNode value : blocks) {
            String identifier = value.stringValue();
            if (identifier == null) {
                throw new IllegalArgumentException(name + ".blocks entries must be strings");
            }

            BlockType block = blockResolver.apply(Identifier.parse(identifier))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown block '" + identifier + "'"));
            result.add(block);
        }

        return List.copyOf(result);
    }

    private static ItemLockMode readItemLock(JsonNode component) {
        if (component == null) {
            return null;
        }

        if (!component.isObject()) {
            throw new IllegalArgumentException(ITEM_LOCK + " must be an object");
        }

        JsonNode modeNode = component.get("mode");
        String mode = modeNode == null ? null : modeNode.stringValue();
        if (mode == null) {
            throw new IllegalArgumentException(ITEM_LOCK + ".mode must be a string");
        }

        return switch (mode) {
            case "lock_in_slot" -> ItemLockMode.LOCK_IN_SLOT;
            case "lock_in_inventory" -> ItemLockMode.LOCK_IN_INVENTORY;
            default -> throw new IllegalArgumentException("Invalid item lock mode '" + mode + "'");
        };
    }

    private static boolean readKeepOnDeath(JsonNode component) {
        if (component == null) {
            return false;
        }

        if (!component.isObject()) {
            throw new IllegalArgumentException(KEEP_ON_DEATH + " must be an object");
        }

        return true;
    }

    /**
     * Applies these components to an item stack builder.
     *
     * @param builder item stack builder to update
     */
    public void applyTo(ItemStackBuilder builder) {
        Objects.requireNonNull(builder, "builder");

        if (this.canDestroy != null) {
            builder.setData(ItemDataComponents.CAN_DESTROY, this.canDestroy);
        }

        if (this.canPlaceOn != null) {
            builder.setData(ItemDataComponents.CAN_PLACE_ON, this.canPlaceOn);
        }

        if (this.itemLock != null) {
            builder.setData(ItemDataComponents.ITEM_LOCK, this.itemLock);
        }

        if (this.keepOnDeath) {
            builder.setData(ItemDataComponents.KEEP_ON_DEATH, true);
        }
    }
}
