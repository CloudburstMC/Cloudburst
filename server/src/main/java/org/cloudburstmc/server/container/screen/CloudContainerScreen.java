package org.cloudburstmc.server.container.screen;

import com.google.common.collect.ImmutableSet;
import org.cloudburstmc.api.container.ContainerScreenType;
import org.cloudburstmc.api.container.ContainerViewType;
import org.cloudburstmc.api.container.screen.ContainerScreen;
import org.cloudburstmc.api.container.view.ContainerView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.*;

import static java.util.Objects.requireNonNull;

public abstract class CloudContainerScreen implements ContainerScreen {

    private final Map<ContainerSlotType, ContainerMapping> mappings = new HashMap<>();
    private final Map<ContainerViewType<?>, ContainerView> views = new HashMap<>();
    protected final CloudPlayer player;

    public CloudContainerScreen(ContainerScreenType<?> type, CloudPlayer player) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(player, "player");
        if (!type.getScreenClass().isInstance(this)) {
            throw new IllegalArgumentException("Screen type " + type.getIdentifier() + " is not compatible with " + this.getClass().getName());
        }
        this.player = player;
    }

    public final void setup() {
        this.setupMappings();
    }

    protected void addMapping(ContainerMapping mapping) {
        this.mappings.put(mapping.getSlotType(), mapping);
        this.views.putIfAbsent(mapping.getView().getViewType(), mapping.getView());
    }

    protected abstract void setupMappings();

    public void close() {
    }

    public ItemStack getSlot(ContainerSlotType type, int slot) {
        ContainerMapping mapping = this.mappings.get(type);
        requireNonNull(mapping, "View for slot type " + type + " not found");

        int realSlot = mapping.getInventorySlot(slot);
        return mapping.getView().getItem(realSlot);
    }

    public void setSlot(ContainerSlotType type, int slot, ItemStack item) {
        ContainerMapping mapping = this.mappings.get(type);
        requireNonNull(mapping, "View for slot type " + type + " not found");

        int realSlot = mapping.getInventorySlot(slot);
        mapping.getView().setItem(realSlot, item);
    }

    @Override
    public ContainerScreenType<?> getType() {
        return null;
    }

    @Override
    public Set<ContainerViewType<?>> getViewTypes() {
        return ImmutableSet.copyOf(this.views.keySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ContainerView> Optional<T> getView(ContainerViewType<T> type) {
        return Optional.ofNullable((T) this.views.get(type));
    }

    @Override
    public Set<ContainerView> getViews() {
        return ImmutableSet.copyOf(this.views.values());
    }
}
