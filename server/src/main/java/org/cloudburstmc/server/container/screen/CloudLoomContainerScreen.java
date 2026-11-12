package org.cloudburstmc.server.container.screen;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.inventory.PrepareResultEvent;
import org.cloudburstmc.api.event.player.PlayerLoomPatternSelectEvent;
import org.cloudburstmc.api.inventory.LoomScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockLoomView;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.BannerData;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudLoomView;
import org.cloudburstmc.server.item.VanillaItemDyes;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.cloudburstmc.api.util.data.BannerPattern.Type.*;

public class CloudLoomContainerScreen extends CloudBlockContainerScreen implements LoomScreen, ResultContainerScreen {

    public static final int BANNER_UI_SLOT = 9;
    public static final int DYE_UI_SLOT = 10;
    private static final int PATTERN_UI_SLOT = 11;
    private static final int RESULT_UI_SLOT = 50;
    private static final int MAX_LAYERS = 6;
    private static final Map<BannerPattern.Type, ItemType> PATTERN_ITEMS = Map.of(
            PATTERN_BRICK, ItemTypes.FIELD_MASONED_BANNER_PATTERN,
            PATTERN_CURLY_BORDER, ItemTypes.BORDURE_INDENTED_BANNER_PATTERN,
            PATTERN_FLOWER, ItemTypes.FLOWER_BANNER_PATTERN,
            PATTERN_CREEPER, ItemTypes.CREEPER_BANNER_PATTERN,
            PATTERN_SKULL, ItemTypes.SKULL_BANNER_PATTERN,
            PATTERN_MOJANG, ItemTypes.MOJANG_BANNER_PATTERN,
            PATTERN_GLOBE, ItemTypes.GLOBE_BANNER_PATTERN,
            PATTERN_PIGLIN, ItemTypes.PIGLIN_BANNER_PATTERN,
            PATTERN_FLOW, ItemTypes.FLOW_BANNER_PATTERN,
            PATTERN_GUSTER, ItemTypes.GUSTER_BANNER_PATTERN
    );

    private final CloudLoomView loom;
    private boolean closed;
    private int lastSoundTick = -1;

    public CloudLoomContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.LOOM, player, block);
        this.loom = new CloudLoomView(block);
    }

    @Override
    public BlockLoomView getLoom() {
        return this.loom;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        Inputs inputs = this.inputs();
        this.loom.setBanner(ItemStack.EMPTY);
        this.loom.setDye(ItemStack.EMPTY);
        this.loom.setPattern(ItemStack.EMPTY);
        super.close();

        for (ItemStack stack : List.of(inputs.banner(), inputs.dye(), inputs.material())) {
            if (!stack.isEmpty()) {
                for (ItemStack leftover : this.player.getContainer().addItem(stack)) {
                    this.player.dropItem(leftover);
                }
            }
        }
    }

    @Override
    protected Container getStorageContainer() {
        return this.loom.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new UIContainerMapping(ContainerSlotType.LOOM_INPUT, this.loom, CloudLoomView.BANNER_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.LOOM_DYE, this.loom, CloudLoomView.DYE_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.LOOM_MATERIAL, this.loom, CloudLoomView.PATTERN_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.LOOM_RESULT, this.loom, CloudLoomView.RESULT_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.CREATED_OUTPUT, this.loom, CloudLoomView.RESULT_SLOT));
    }

    @Override
    protected @Nullable FullContainerName uiSlotContainerName(ContainerSlotType type) {
        return switch (type) {
            case LOOM_INPUT, LOOM_DYE, LOOM_MATERIAL, LOOM_RESULT, CREATED_OUTPUT -> null;
            default -> super.uiSlotContainerName(type);
        };
    }

    @Override
    public boolean isInputSlot(ContainerSlotType type) {
        return type == ContainerSlotType.LOOM_INPUT || type == ContainerSlotType.LOOM_DYE || type == ContainerSlotType.LOOM_MATERIAL;
    }

    @Override
    public boolean isResultSlot(ContainerSlotType type, int slot) {
        return slot == RESULT_UI_SLOT && (type == ContainerSlotType.LOOM_RESULT || type == ContainerSlotType.CREATED_OUTPUT);
    }

    @Override
    public void trackResultSlots(BiConsumer<ContainerSlotType, Integer> tracker) {
        tracker.accept(ContainerSlotType.LOOM_INPUT, BANNER_UI_SLOT);
        tracker.accept(ContainerSlotType.LOOM_DYE, DYE_UI_SLOT);
        tracker.accept(ContainerSlotType.LOOM_MATERIAL, PATTERN_UI_SLOT);
        tracker.accept(ContainerSlotType.LOOM_RESULT, RESULT_UI_SLOT);
    }

    @Override
    public @Nullable ResultTake prepareTake(ItemStack expected, int count) {
        return null;
    }

    public boolean rejectsInput(ContainerSlotType type, ItemStack item) {
        return !item.isEmpty() && switch (type) {
            case LOOM_INPUT -> item.getType() != ItemTypes.BANNER;
            case LOOM_DYE -> VanillaItemDyes.color(item.getType()) == null;
            case LOOM_MATERIAL -> !PATTERN_ITEMS.containsValue(item.getType());
            default -> false;
        };
    }

    public @Nullable ResultQuote quotePattern(String patternId, int requestedCrafts) {
        Inputs inputs = this.inputs();
        BannerPattern.Type pattern = BannerPattern.Type.getByName(patternId);
        if (pattern == null || requestedCrafts <= 0 || this.isUnavailable(inputs)) {
            return null;
        }

        ItemType required = PATTERN_ITEMS.get(pattern);
        if (required != null ? inputs.material().getType() != required : !inputs.material().isEmpty()) {
            return null;
        }

        BannerData existing = inputs.banner().getOrDefault(ItemDataComponents.BANNER_DATA,
                BannerData.of(0, inputs.banner().getOrDefault(ItemDataComponents.COLOR, DyeColor.BLACK), List.of()));
        if (existing.getType() != 0 || existing.getPatterns().size() >= MAX_LAYERS) {
            return null;
        }

        if (pattern != this.loom.getSelectedPattern()) {
            PlayerLoomPatternSelectEvent event = new PlayerLoomPatternSelectEvent(this.player, this.loom, pattern);
            this.player.getServer().getEventManager().fire(event);
            if (event.isCancelled() || this.isUnavailable(inputs)) {
                return null;
            }
            pattern = event.getPatternType();
        }

        String dyeId = inputs.dye().getType().getId().getName();
        DyeColor color = DyeColor.valueOf(dyeId.substring(0, dyeId.length() - 4).toUpperCase(Locale.ROOT));
        List<BannerPattern> layers = new ArrayList<>(existing.getPatterns());
        layers.add(new BannerPattern(pattern, color));
        ItemStack result = inputs.banner().toBuilder()
                .amount(1)
                .setData(ItemDataComponents.BANNER_DATA, BannerData.of(existing.getType(), existing.getBase(), layers))
                .build();

        PrepareResultEvent prepare = new PrepareResultEvent(this, result);
        this.player.getServer().getEventManager().fire(prepare);
        ItemStack output = prepare.getResult().normalizeBlockState();
        if (output.isEmpty() || this.isUnavailable(inputs)) {
            return null;
        }

        BannerPattern.Type selected = pattern;
        int maxCrafts = Math.min(requestedCrafts, Math.min(inputs.banner().getCount(), inputs.dye().getCount()));
        return new ResultQuote(output,
                count -> isInvalidCount(output, count, maxCrafts),
                count -> this.prepareCraft(inputs, selected, output, count, maxCrafts));
    }

    private @Nullable ResultTake prepareCraft(Inputs inputs, BannerPattern.Type pattern, ItemStack output, int count, int maxCrafts) {
        if (this.isUnavailable(inputs) || isInvalidCount(output, count, maxCrafts)) {
            return null;
        }

        int crafts = count / output.getCount();
        return new ResultTake(output.withCount(count),
                () -> this.loom.finishCraft(remaining(inputs.banner(), crafts), remaining(inputs.dye(), crafts), pattern, output),
                this::playResultSound
        );
    }

    private static boolean isInvalidCount(ItemStack output, int count, int maxCrafts) {
        return count <= 0 || count % output.getCount() != 0 || count / output.getCount() > maxCrafts;
    }

    private boolean isUnavailable(Inputs inputs) {
        return this.closed
                || this.player.getInventoryManager().getScreen() != this
                || this.player.getLevel() != this.block.getLevel()
                || !this.player.isAlive() || this.player.isSpectator()
                || !this.player.canInteractWithBlock(this.block.getPosition())
                || this.player.getLevel().getBlockState(this.block.getPosition()).getType() != BlockTypes.LOOM
                || inputs.banner().getType() != ItemTypes.BANNER
                || VanillaItemDyes.color(inputs.dye().getType()) == null
                || inputs.banner().get(ItemDataComponents.ITEM_LOCK) != null
                || inputs.dye().get(ItemDataComponents.ITEM_LOCK) != null
                || !this.inputs().equals(inputs);
    }

    private Inputs inputs() {
        return new Inputs(this.loom.getBanner(), this.loom.getDye(), this.loom.getPattern());
    }

    private void playResultSound() {
        int tick = this.player.getServer().getTick();
        if (this.lastSoundTick != tick) {
            this.lastSoundTick = tick;
            this.player.getLevel().addLevelSoundEvent(this.block.getPosition(), SoundEvent.UI_LOOM_USE);
        }
    }

    private static ItemStack remaining(ItemStack input, int count) {
        return count == input.getCount() ? ItemStack.EMPTY : input.withCount(input.getCount() - count);
    }

    private record Inputs(ItemStack banner, ItemStack dye, ItemStack material) {
    }
}
