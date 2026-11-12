package org.cloudburstmc.server.container.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.inventory.view.BlockLoomView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BannerPattern.Type;
import org.cloudburstmc.server.container.CloudContainer;

public class CloudLoomView extends CloudSlotGroupBase implements BlockLoomView {

    public static final int BANNER_SLOT = 0;
    public static final int DYE_SLOT = 1;
    public static final int PATTERN_SLOT = 2;
    public static final int RESULT_SLOT = 3;

    private final Block block;
    private @Nullable Type selectedPattern;

    public CloudLoomView(Block block) {
        super(SlotGroupTypes.LOOM, new CloudContainer(RESULT_SLOT + 1));
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return this.block;
    }

    @Override
    public ItemStack getBanner() {
        return this.getItem(BANNER_SLOT);
    }

    @Override
    public void setBanner(ItemStack item) {
        this.setItem(BANNER_SLOT, item);
    }

    @Override
    public ItemStack getDye() {
        return this.getItem(DYE_SLOT);
    }

    @Override
    public void setDye(ItemStack item) {
        this.setItem(DYE_SLOT, item);
    }

    @Override
    public ItemStack getPattern() {
        return this.getItem(PATTERN_SLOT);
    }

    @Override
    public void setPattern(ItemStack item) {
        this.setItem(PATTERN_SLOT, item);
    }

    @Override
    public ItemStack getResult() {
        return this.getItem(RESULT_SLOT);
    }

    @Override
    public @Nullable Type getSelectedPattern() {
        return this.selectedPattern;
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        if (slot == RESULT_SLOT) {
            throw new IllegalArgumentException("Cannot modify a loom result directly");
        }

        ItemStack previous = this.getItem(slot);
        super.setItem(slot, item);
        if (!previous.equals(this.getItem(slot))) {
            this.updateResult(null, ItemStack.EMPTY);
        }
    }

    public void finishCraft(ItemStack banner, ItemStack dye, Type pattern, ItemStack output) {
        super.setItem(BANNER_SLOT, banner);
        super.setItem(DYE_SLOT, dye);
        if (banner.isEmpty() || dye.isEmpty()) {
            this.updateResult(null, ItemStack.EMPTY);
        } else {
            this.updateResult(pattern, output);
        }
    }

    private void updateResult(@Nullable Type pattern, ItemStack result) {
        this.selectedPattern = pattern;
        super.setItem(RESULT_SLOT, result);
    }
}
