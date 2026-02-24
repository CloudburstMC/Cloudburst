package org.cloudburstmc.server.container.screen;

import com.google.common.collect.ImmutableSet;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.inventory.InventoryScreen;
import org.cloudburstmc.api.inventory.ScreenType;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Root abstract base class for all server-side inventory screen implementations. Holds the protocol
 * slot-to-{@link ContainerMapping} table and the slot-group-type-to-{@link org.cloudburstmc.api.inventory.view.SlotGroup} section map.
 * Subclasses populate both maps during the {@link #setup()} / {@link #setupMappings()} lifecycle.
 */
public abstract class CloudInventoryScreen implements InventoryScreen {

    protected final CloudPlayer player;
    private final Map<ContainerSlotType, ContainerMapping> mappings = new HashMap<>();
    private final Map<SlotGroupType<?>, SlotGroup> sections = new HashMap<>();
    private final ScreenType<?> type;

    @Nullable
    private String titleOverride;
    private Set<SlotGroupType<?>> cachedSlotGroupTypes;
    private Set<SlotGroup> cachedAllSlotGroups;

    public CloudInventoryScreen(ScreenType<?> type, CloudPlayer player) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(player, "player");
        if (!type.getScreenClass().isInstance(this)) {
            throw new IllegalArgumentException("Screen type " + type.getIdentifier() + " is not compatible with " + this.getClass().getName());
        }
        this.player = player;
        this.type = type;
    }

    public final void setup() {
        this.setupMappings();
        this.cachedSlotGroupTypes = ImmutableSet.copyOf(this.sections.keySet());
        this.cachedAllSlotGroups = ImmutableSet.copyOf(this.sections.values());
    }

    public void open() {
    }

    protected void addMapping(ContainerMapping mapping) {
        this.mappings.put(mapping.getSlotType(), mapping);
        this.sections.putIfAbsent(mapping.getView().getSlotGroupType(), mapping.getView());
    }

    protected abstract void setupMappings();

    public void close() {
    }

    public ItemStack getSlot(ContainerSlotType type, int slot) {
        ContainerMapping mapping = this.mappings.get(type);
        requireNonNull(mapping, "Screen for slot type " + type + " not found");

        int realSlot = mapping.getInventorySlot(slot);
        return mapping.getView().getItem(realSlot);
    }

    public void setSlot(ContainerSlotType type, int slot, ItemStack item) {
        ContainerMapping mapping = this.mappings.get(type);
        requireNonNull(mapping, "Screen for slot type " + type + " not found");

        int realSlot = mapping.getInventorySlot(slot);
        mapping.getView().setItem(realSlot, item);
    }

    @Nullable
    public SlotGroup resolveSlotGroup(ContainerSlotType type) {
        ContainerMapping mapping = this.mappings.get(type);
        return mapping != null ? mapping.getView() : null;
    }

    public int resolveInventorySlot(ContainerSlotType type, int screenSlot) {
        ContainerMapping mapping = this.mappings.get(type);
        return mapping != null ? mapping.getInventorySlot(screenSlot) : -1;
    }

    @Override
    public org.cloudburstmc.api.player.Player getPlayer() {
        return player;
    }

    @Override
    public ScreenType<?> getType() {
        return this.type;
    }

    @Override
    public Set<SlotGroupType<?>> getSlotGroupTypes() {
        return cachedSlotGroupTypes;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends SlotGroup> Optional<T> getSlots(SlotGroupType<T> type) {
        return Optional.ofNullable((T) this.sections.get(type));
    }

    @Override
    public Set<SlotGroup> getAllSlotGroups() {
        return cachedAllSlotGroups;
    }

    @Override
    @Nullable
    public String getTitle() {
        return titleOverride;
    }

    @Nullable
    public String getTitleOverride() {
        return titleOverride;
    }

    public void setTitleOverride(@Nullable String titleOverride) {
        this.titleOverride = titleOverride;
    }
}
