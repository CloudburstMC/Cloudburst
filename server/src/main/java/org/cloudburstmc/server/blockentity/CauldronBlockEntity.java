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
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.potion.CloudPotionColor;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CauldronBlockEntity extends BaseBlockEntity implements Cauldron {

    public CauldronBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return CauldronData.class;
    }

    @Override
    protected CauldronData data() {
        return (CauldronData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        // The custom color is derived at save time: a potion always carries the
        // color of its effects, dye is stored masked. Normalizing on load keeps
        // the DTO save-ready, exactly like the previous fillData did.
        CauldronData cauldron = data();
        PotionType potion = cauldron.getPotionId() < 0 ? null : NetworkUtils.potionFromNetwork(cauldron.getPotionId());
        if (potion != null) {
            cauldron.setCustomColor(0xFF000000 | CloudPotionColor.calculateEffects(potion.getEffects()).orElse(0x2828FF));
        } else if (cauldron.getCustomColor() != null) {
            cauldron.setCustomColor(0xFF000000 | (cauldron.getCustomColor() & 0xFFFFFF));
        }
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
        CauldronData cauldron = data();
        short potionId = cauldron.getPotionId();
        PotionType potion = potionId < 0 ? null : NetworkUtils.potionFromNetwork(potionId);
        PotionForm form = potion == null ? PotionForm.DRINKABLE : switch (cauldron.getPotionType()) {
            case 1 -> PotionForm.SPLASH;
            case 2 -> PotionForm.LINGERING;
            default -> PotionForm.DRINKABLE;
        };

        Integer color = potion == null && cauldron.getCustomColor() != null
                ? cauldron.getCustomColor() & 0xFFFFFF : null;
        return new CauldronContents(potion, form, color);
    }

    @Override
    public void setContents(CauldronContents contents) {
        Objects.requireNonNull(contents, "contents");

        if (!contents.equals(getContents())) {
            CauldronData cauldron = data();
            PotionType potion = contents.potion();
            short potionId = potion == null ? (short) -1 : NetworkUtils.potionToNetwork(potion);
            short formId = -1;
            if (potion != null) {
                formId = (short) switch (contents.form()) {
                    case DRINKABLE -> 0;
                    case SPLASH -> 1;
                    case LINGERING -> 2;
                };
            }
            cauldron.setPotionId(potionId);
            cauldron.setPotionType(formId);

            Integer color = contents.color();
            if (potion != null) {
                color = CloudPotionColor.calculateEffects(potion.getEffects()).orElse(0x2828FF);
            }
            cauldron.setCustomColor(color == null ? null : 0xFF000000 | color);
            this.setDirty();
            this.spawnToAll();
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
