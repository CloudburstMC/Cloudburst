package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Cauldron;
import org.cloudburstmc.api.blockentity.CauldronContents;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.potion.PotionForm;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.potion.CloudPotionColor;

import java.util.List;
import java.util.Objects;

public class CauldronBlockEntity extends BaseBlockEntity implements Cauldron {

    private CauldronContents contents = CauldronContents.PLAIN;

    public CauldronBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        short potionId = tag.getShort("PotionId", (short) -1);
        PotionType potion = potionId < 0 ? null : NetworkUtils.potionFromNetwork(potionId);
        PotionForm form = potion == null ? PotionForm.DRINKABLE : switch (tag.getShort("PotionType", (short) -1)) {
            case 1 -> PotionForm.SPLASH;
            case 2 -> PotionForm.LINGERING;
            default -> PotionForm.DRINKABLE;
        };

        Integer color = potion == null && tag.containsKey("CustomColor") ? tag.getInt("CustomColor") & 0xFFFFFF : null;
        this.contents = new CauldronContents(potion, form, color);
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);

        PotionType potion = this.contents.potion();
        short potionId = potion == null ? (short) -1 : NetworkUtils.potionToNetwork(potion);
        short formId = -1;
        if (potion != null) {
            formId = (short) switch (this.contents.form()) {
                case DRINKABLE -> 0;
                case SPLASH -> 1;
                case LINGERING -> 2;
            };
        }

        tag.putShort("PotionId", potionId);
        tag.putShort("PotionType", formId);
        tag.putList("Items", NbtType.END, List.of());
        tag.remove("IsSplash");

        Integer color = this.contents.color();
        if (potion != null) {
            color = CloudPotionColor.calculateEffects(potion.getEffects()).orElse(0x2828FF);
        }

        if (color == null) {
            tag.remove("CustomColor");
        } else {
            tag.putInt("CustomColor", 0xFF000000 | color);
        }
    }

    @Override
    public CauldronContents getContents() {
        return this.contents;
    }

    @Override
    public void setContents(CauldronContents contents) {
        Objects.requireNonNull(contents, "contents");

        if (!this.contents.equals(contents)) {
            this.contents = contents;
            this.setDirty();
            this.spawnToAll();
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
