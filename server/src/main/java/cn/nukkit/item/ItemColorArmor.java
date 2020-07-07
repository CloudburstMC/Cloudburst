package cn.nukkit.item;

import cn.nukkit.utils.BlockColor;
import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.Identifier;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;

/**
 * Created by fromgate on 27.03.2016.
 */
abstract public class ItemColorArmor extends ItemArmor {

    private BlockColor color = BlockColor.WHITE_BLOCK_COLOR;

    public ItemColorArmor(Identifier id) {
        super(id);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForInt("customColor", this::setColor);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putInt("customColor", this.getColor().getRGB());
    }

    /**
     * Get color of Leather Item
     *
     * @return - BlockColor, or null if item has no color
     */
    public BlockColor getColor() {
        return this.color;
    }

    /**
     * Set leather armor color
     *
     * @param dyeColor - DyeColor object
     * @return - Return colored item
     */
    public ItemColorArmor setColor(DyeColor dyeColor) {
        return setColor(dyeColor.getColor());
    }

    /**
     * Set leather armor color
     *
     * @param color - BlockColor object
     * @return - Return colored item
     */
    public ItemColorArmor setColor(BlockColor color) {
        this.color = color;
        return this;
    }

    /**
     * Set leather armor color
     *
     * @param rgb - red, green, blue
     * @return - Return colored item
     */
    public ItemColorArmor setColor(int rgb) {
        this.color = new BlockColor(rgb);
        return this;
    }
}
