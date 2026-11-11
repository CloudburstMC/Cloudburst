package org.cloudburstmc.server.container.screen;

import com.google.common.collect.ImmutableSet;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.inventory.InventoryScreen;
import org.cloudburstmc.api.inventory.ScreenType;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.packet.ContainerOpenPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudSlotGroupBase;
import org.cloudburstmc.server.item.ItemStackPacketCodec;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Maps network slots to inventory sections and observes containers displayed by the screen.
 * Subclasses define their mappings in {@link #setupMappings()} before {@link #setup()} completes.
 */
public abstract class CloudInventoryScreen implements InventoryScreen {

    protected final CloudPlayer player;
    private final Map<ContainerSlotType, ContainerMapping> mappings = new HashMap<>();
    private final Map<SlotGroupType<?>, SlotGroup> sections = new HashMap<>();
    private final Set<CloudContainer> observedContainers = new HashSet<>();
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

    protected final void openWindow(ContainerOpenPacket packet) {
        this.openWindow(packet, null);
    }

    protected final void openWindow(ContainerOpenPacket packet, @Nullable Container storage) {
        this.player.getInventoryManager().openWindow(this, packet, storage);

        if (storage instanceof CloudContainer container) {
            this.observeContainer(container);
        }

        for (ContainerMapping mapping : this.mappings.values()) {
            if (mapping instanceof UIContainerMapping && mapping.getView() instanceof CloudSlotGroupBase group) {
                this.observeContainer(group.getContainer());
            }
        }
    }

    private void observeContainer(CloudContainer container) {
        if (!container.getListeners().contains(this.player)) {
            this.observedContainers.add(container);
            container.addContainerListener(this.player);
        }
    }

    public final void stopObservingContainers() {
        for (CloudContainer container : this.observedContainers) {
            container.removeContainerListener(this.player);
        }

        this.observedContainers.clear();
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

    public void sendSlot(ContainerSlotType type, int slot) {
        ContainerMapping mapping = requireNonNull(this.mappings.get(type), "mapping");
        if (mapping instanceof UIContainerMapping) {
            InventorySlotPacket packet = new InventorySlotPacket();
            packet.setContainerId(ContainerId.UI);
            packet.setContainerNameData(uiSlotContainerName(type));
            packet.setSlot(slot);
            packet.setItem(ItemStackPacketCodec.toNetworkNetId(this.getSlot(type, slot)));
            this.player.sendPacket(packet);
        } else if (this.resolveSlotGroup(type) instanceof CloudSlotGroupBase group) {
            int inventorySlot = this.resolveInventorySlot(type, slot);
            this.player.onInventorySlotChange(group.getContainer(), group.getContainerSlot(inventorySlot));
        }
    }

    protected @Nullable FullContainerName uiSlotContainerName(ContainerSlotType type) {
        return new FullContainerName(type, null);
    }

    public boolean sendUIContainerSlot(Container container, int containerSlot) {
        Map<Integer, ContainerMapping> slotMappings = new TreeMap<>();
        for (ContainerMapping mapping : this.mappings.values()) {
            if (!(mapping instanceof UIContainerMapping) || !(mapping.getView() instanceof CloudSlotGroupBase group) || group.getContainer() != container) {
                continue;
            }

            int inventorySlot = containerSlot - group.getContainerSlot(0);
            if (mapping.containsInventorySlot(inventorySlot)) {
                int protocolSlot = mapping.getProtocolSlot(inventorySlot);
                slotMappings.merge(protocolSlot, mapping, (existing, candidate) -> existing.getSlotType() == ContainerSlotType.CREATED_OUTPUT ? candidate : existing);
            }
        }

        slotMappings.forEach((slot, mapping) -> this.sendSlot(mapping.getSlotType(), slot));
        return !slotMappings.isEmpty();
    }

    public boolean sendUIContainerContents(Container container) {
        boolean sent = false;
        for (int slot = 0; slot < container.size(); slot++) {
            sent |= this.sendUIContainerSlot(container, slot);
        }
        return sent;
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

    public int resolveProtocolSlot(ContainerSlotType type, int inventorySlot) {
        ContainerMapping mapping = this.mappings.get(type);
        return mapping != null && mapping.containsInventorySlot(inventorySlot) ? mapping.getProtocolSlot(inventorySlot) : -1;
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
    public Component getTitle() {
        return titleOverride != null ? BedrockLegacyTextSerializer.getInstance().deserialize(titleOverride) : null;
    }

    @Nullable
    public String getTitleOverride() {
        return titleOverride;
    }

    public void setTitleOverride(@Nullable Component component) {
        this.titleOverride = component != null ? BedrockLegacyTextSerializer.getInstance().serialize(component) : null;
    }
}
