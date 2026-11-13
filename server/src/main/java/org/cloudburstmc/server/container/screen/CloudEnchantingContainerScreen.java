package org.cloudburstmc.server.container.screen;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.event.enchantment.EnchantItemEvent;
import org.cloudburstmc.api.event.enchantment.PrepareItemEnchantEvent;
import org.cloudburstmc.api.inventory.EnchantingScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.EnchantingView;
import org.cloudburstmc.api.inventory.view.EnchantmentOffer;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.EnchantData;
import org.cloudburstmc.protocol.bedrock.data.inventory.EnchantOptionData;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.packet.PlayerEnchantOptionsPacket;
import org.cloudburstmc.server.block.component.EnchantingTableBlockHandlers;
import org.cloudburstmc.server.blockentity.EnchantingTableBlockEntity;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudEnchantingView;
import org.cloudburstmc.server.enchantment.TableEnchantments;
import org.cloudburstmc.server.network.inventory.ItemStackNetworkIds;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;

import java.util.*;

public class CloudEnchantingContainerScreen extends CloudBlockContainerScreen implements EnchantingScreen {

    public static final int INPUT_UI_SLOT = 14;
    public static final int REAGENT_UI_SLOT = 15;

    private static final int PRIMARY_ENCHANTMENT_SLOT = 16;
    private static final List<String> OFFER_NAMES = List.of("the scrolls", "light within", "imbue power");

    private final CloudEnchantingView enchantSection;
    private final int[] offerIds = new int[TableEnchantments.OFFER_COUNT];
    private ItemStack preparedInput = ItemStack.EMPTY;
    private int preparedPower;
    private boolean updating;
    private boolean closed;
    private boolean offersDirty;

    public CloudEnchantingContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.ENCHANTING, player, block);
        this.enchantSection = new CloudEnchantingView(this::prepareOffers, player::getEnchantmentSeed);
    }

    @Override
    public EnchantingView getEnchanting() {
        return this.enchantSection;
    }

    @Override
    public void open() {
        Block block = this.getBlock();
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof EnchantingTableBlockEntity table && table.hasCustomName()) {
            table.spawnTo(this.player);
        }
        this.player.sendPacket(new PlayerEnchantOptionsPacket());
        super.open();
        if (!this.enchantSection.getInput().isEmpty()) {
            this.prepareOffers();
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        List<ItemStack> inputs = List.of(this.enchantSection.getInput(), this.enchantSection.getReagent());
        this.enchantSection.setInput(ItemStack.EMPTY);
        this.enchantSection.setReagent(ItemStack.EMPTY);
        this.enchantSection.updateOffers(Collections.nCopies(TableEnchantments.OFFER_COUNT, null));
        super.close();

        for (ItemStack input : inputs) {
            if (!input.isEmpty()) {
                for (ItemStack remaining : this.player.getContainer().addItem(input)) {
                    this.player.dropItem(remaining);
                }
            }
        }
    }

    @Override
    protected Container getStorageContainer() {
        return this.enchantSection.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new UIContainerMapping(ContainerSlotType.ENCHANTING_INPUT, this.enchantSection, CloudEnchantingView.INPUT_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.ENCHANTING_MATERIAL, this.enchantSection, CloudEnchantingView.REAGENT_SLOT));
    }

    public boolean acceptsItem(ContainerSlotType type, ItemStack item) {
        return item.isEmpty() || switch (type) {
            case ENCHANTING_INPUT -> item.getCount() == 1;
            case ENCHANTING_MATERIAL -> item.getType() == ItemTypes.LAPIS_LAZULI && item.getCount() <= 64;
            default -> true;
        };
    }

    @Override
    protected @Nullable FullContainerName uiSlotContainerName(ContainerSlotType type) {
        return switch (type) {
            case ENCHANTING_INPUT, ENCHANTING_MATERIAL -> null;
            default -> super.uiSlotContainerName(type);
        };
    }

    public boolean selectOffer(int netId) {
        if (netId <= 0 || this.updating || this.isTableUnavailable()) {
            return false;
        }

        ItemStack input = this.enchantSection.getInput();
        int power = this.bookshelfPower();
        if (!input.equals(this.preparedInput) || power != this.preparedPower) {
            this.prepareOffers();
            return false;
        }

        for (int button = 0; button < this.offerIds.length; button++) {
            if (this.offerIds[button] == netId) {
                EnchantmentOffer offer = this.enchantSection.getOffers().get(button);
                return offer != null && this.enchant(input, button, offer);
            }
        }

        return false;
    }

    private boolean enchant(ItemStack input, int button, EnchantmentOffer offer) {
        int cost = button + 1;
        ItemStack reagent = this.enchantSection.getReagent();
        if (this.lacksEnchantingResources(reagent, cost, offer.requiredLevel())) {
            return false;
        }

        EnchantItemEvent event = new EnchantItemEvent(this, input, button, offer);
        this.updating = true;
        try {
            this.player.getServer().getEventManager().fire(event);
            if (event.isCancelled()
                    || this.isTableUnavailable()
                    || !input.equals(this.enchantSection.getInput())
                    || !reagent.equals(this.enchantSection.getReagent())
                    || this.bookshelfPower() != this.preparedPower
                    || this.lacksEnchantingResources(reagent, cost, event.getRequiredLevel())) {
                return false;
            }

            Map<EnchantmentType, Enchantment> selected = new LinkedHashMap<>();
            for (Map.Entry<EnchantmentType, Integer> entry : event.getEnchantsToAdd().entrySet()) {
                EnchantmentType type = entry.getKey();
                Integer level = entry.getValue();
                if (type == null || level == null || level < 1 || level > Short.MAX_VALUE || !isRegistered(type)) {
                    return false;
                }
                selected.put(type, new Enchantment(type, level));
            }

            if (selected.isEmpty()) {
                return false;
            }

            ItemStackBuilder output = input.toBuilder().setData(ItemDataComponents.ENCHANTMENTS, selected);
            if (input.getType() == ItemTypes.BOOK) {
                output.itemType(ItemTypes.ENCHANTED_BOOK);
            }

            ItemStack enchanted = output.build();
            boolean creative = this.player.isCreative();
            CloudContainer.applySlotWrites(() -> {
                this.enchantSection.setInput(enchanted);
                if (!creative) {
                    this.enchantSection.setReagent(reagent.decreaseCount(cost));
                }
            });

            this.player.completeEnchantment(creative ? 0 : cost);
            this.player.getLevel().addLevelSoundEvent(this.getBlock().getPosition(), SoundEvent.ENCHANTING_TABLE_USE);
            return true;
        } finally {
            this.updating = false;
            if (!input.equals(this.enchantSection.getInput())) {
                this.prepareOffers();
            }
        }
    }

    private boolean lacksEnchantingResources(ItemStack reagent, int cost, int requiredLevel) {
        return !this.player.isCreative() && (this.player.getExperienceLevel() < Math.max(requiredLevel, cost)
                || reagent.getType() != ItemTypes.LAPIS_LAZULI || reagent.getCount() < cost);
    }

    private void prepareOffers() {
        if (this.closed || this.updating) {
            return;
        }

        this.updating = true;
        try {
            ItemStack input = this.enchantSection.getInput();
            int power = this.bookshelfPower();
            List<@Nullable EnchantmentOffer> offers = TableEnchantments.generate(input, power, this.player.getEnchantmentSeed());

            if (!input.isEmpty()) {
                PrepareItemEnchantEvent event = new PrepareItemEnchantEvent(this, input, power, offers);
                event.setCancelled(offers.stream().allMatch(Objects::isNull));
                this.player.getServer().getEventManager().fire(event);
                offers = event.isCancelled() ? Collections.nCopies(TableEnchantments.OFFER_COUNT, null) : event.getOffers();
            }

            if (this.closed) {
                return;
            }

            if (input.getCount() != 1 || !input.equals(this.enchantSection.getInput())) {
                offers = Collections.nCopies(TableEnchantments.OFFER_COUNT, null);
            }

            List<@Nullable EnchantmentOffer> prepared = new ArrayList<>(offers);
            prepared.replaceAll(offer -> {
                if (offer == null || offer.requiredLevel() > 255) {
                    return null;
                }

                return offer.enchantments().stream().allMatch(enchantment -> enchantment.level() <= 255 && isRegistered(enchantment.type())) ? offer : null;
            });

            this.preparedInput = input;
            this.preparedPower = power;
            this.enchantSection.updateOffers(prepared);
            Arrays.fill(this.offerIds, 0);
            this.offersDirty = true;
            if (!this.player.getInventoryRequestQueue().isProcessingRequests()) {
                this.sendOffers();
            }
        } finally {
            this.updating = false;
        }
    }

    public void sendOffers() {
        if (this.closed || !this.offersDirty) {
            return;
        }

        this.offersDirty = false;
        PlayerEnchantOptionsPacket packet = new PlayerEnchantOptionsPacket();
        if (this.enchantSection.getOffers().stream().allMatch(Objects::isNull)) {
            this.player.sendPacket(packet);
            return;
        }

        for (int i = 0; i < this.offerIds.length; i++) {
            EnchantmentOffer offer = this.enchantSection.getOffers().get(i);
            if (offer == null) {
                this.offerIds[i] = 0;
                packet.getOptions().add(new EnchantOptionData(0, PRIMARY_ENCHANTMENT_SLOT, List.of(), List.of(), List.of(), OFFER_NAMES.get(i), 0));
                continue;
            }

            CloudEnchantmentRegistry registry = CloudEnchantmentRegistry.get();
            List<EnchantData> enchants = offer.enchantments().stream()
                    .map(enchantment -> new EnchantData(registry.getSerializedId(enchantment.type()), enchantment.level()))
                    .toList();
            this.offerIds[i] = ItemStackNetworkIds.allocate();
            packet.getOptions().add(new EnchantOptionData(
                    offer.requiredLevel(), PRIMARY_ENCHANTMENT_SLOT,
                    List.of(), List.of(enchants.getFirst()), enchants.subList(1, enchants.size()),
                    OFFER_NAMES.get(i), this.offerIds[i]));
        }

        this.player.sendPacket(packet);
    }

    private static boolean isRegistered(EnchantmentType type) {
        return CloudEnchantmentRegistry.get().get(type.identifier()).filter(type::equals).isPresent();
    }

    private int bookshelfPower() {
        return EnchantingTableBlockHandlers.countBookshelves(this.player.getLevel(), this.getBlock().getPosition());
    }

    private boolean isTableUnavailable() {
        Block block = this.getBlock();
        return this.closed || block.getLevel() != this.player.getLevel()
                || !this.player.canInteractWithBlock(block.getPosition())
                || this.player.getLevel().getBlockState(block.getPosition()).getType() != BlockTypes.ENCHANTING_TABLE;
    }
}
