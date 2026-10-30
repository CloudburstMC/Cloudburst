package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Cauldron;
import org.cloudburstmc.api.blockentity.CauldronContents;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.CauldronData;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.potion.PotionForm;
import org.cloudburstmc.api.potion.PotionType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.potion.CloudPotionColor;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CauldronBlockEntity extends BaseBlockEntity implements Cauldron {

    private CauldronContents contents = CauldronContents.PLAIN;

    public CauldronBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return CauldronData.class;
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        CauldronData cauldron = (CauldronData) data;
        short potionId = cauldron.getPotionId();
        PotionType potion = potionId < 0 ? null : NetworkUtils.potionFromNetwork(potionId);
        PotionForm form = potion == null ? PotionForm.DRINKABLE : switch (cauldron.getPotionType()) {
            case 1 -> PotionForm.SPLASH;
            case 2 -> PotionForm.LINGERING;
            default -> PotionForm.DRINKABLE;
        };

        Integer color = potion == null && cauldron.getCustomColor() != null
                ? cauldron.getCustomColor() & 0xFFFFFF : null;
        this.contents = new CauldronContents(potion, form, color);
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        CauldronData cauldron = (CauldronData) data;
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
        cauldron.setPotionId(potionId);
        cauldron.setPotionType(formId);

        Integer color = this.contents.color();
        if (potion != null) {
            color = CloudPotionColor.calculateEffects(potion.getEffects()).orElse(0x2828FF);
        }
        cauldron.setCustomColor(color == null ? null : 0xFF000000 | color);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        writeItems(tag);
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        writeItems(tag);
    }

    @Override
    protected Set<String> customKeys() {
        return Set.of("Items", "IsSplash");
    }

    private void writeItems(NbtMapBuilder tag) {
        tag.putList("Items", NbtType.END, List.of());
        tag.remove("IsSplash");
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
