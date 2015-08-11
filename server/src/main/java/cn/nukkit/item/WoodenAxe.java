package cn.nukkit.item;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class WoodenAxe extends Tool {

    public WoodenAxe() {
        this(0, 1);
    }

    public WoodenAxe(int meta) {
        this(meta, 1);
    }

    public WoodenAxe(int meta, int count) {
        super(WOODEN_AXE, meta, count, "Wooden Axe");
    }

    @Override
    public int getMaxDurability() {
        return Tool.DURABILITY_WOODEN;
    }

    @Override
    public boolean isAxe() {
        return true;
    }
}
