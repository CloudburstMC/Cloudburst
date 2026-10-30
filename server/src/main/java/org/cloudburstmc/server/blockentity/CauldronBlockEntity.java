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
        // The potion and form ids plus the custom color are hook-owned: they are
        // NBT-shaped network ids, not the runtime contents this DTO holds. Absent
        // keys default as the DTO fields did (potion and form -1, no color): a
        // potion always carries the color of its effects, dye is stored masked.
        short potionId = numberOr(tag, "PotionId", (short) -1).shortValue();
        short potionType = numberOr(tag, "PotionType", (short) -1).shortValue();
        Integer customColor = tag.get("CustomColor") instanceof Number number ? number.intValue() : null;
        PotionType potion = potionId < 0 ? null : NetworkUtils.potionFromNetwork(potionId);
        PotionForm form = potion == null ? PotionForm.DRINKABLE : switch (potionType) {
            case 1 -> PotionForm.SPLASH;
            case 2 -> PotionForm.LINGERING;
            default -> PotionForm.DRINKABLE;
        };
        Integer color;
        if (potion != null) {
            color = null;
        } else if (customColor != null) {
            color = customColor & 0xFFFFFF;
        } else {
            color = null;
        }
        data().setContents(new CauldronContents(potion, form, color));
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        writeContents(tag);
        writeItems(tag);
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        writeContents(tag);
        writeItems(tag);
    }

    @Override
    protected Set<String> customKeys() {
        return Set.of("PotionId", "PotionType", "CustomColor", "Items", "IsSplash");
    }

    private void writeContents(NbtMapBuilder tag) {
        CauldronContents contents = data().getContents();
        PotionType potion = contents.potion();
        tag.putShort("PotionId", potion == null ? (short) -1 : NetworkUtils.potionToNetwork(potion));
        short formId;
        if (potion == null) {
            formId = -1;
        } else {
            formId = switch (contents.form()) {
                case DRINKABLE -> (short) 0;
                case SPLASH -> (short) 1;
                case LINGERING -> (short) 2;
            };
        }
        tag.putShort("PotionType", formId);
        Integer color;
        if (potion != null) {
            color = CloudPotionColor.calculateEffects(potion.getEffects()).orElse(0x2828FF);
        } else {
            color = contents.color();
        }
        if (color != null) {
            tag.putInt("CustomColor", 0xFF000000 | color);
        }
    }

    private static Number numberOr(NbtMap tag, String key, Number fallback) {
        return tag.get(key) instanceof Number number ? number : fallback;
    }

    private void writeItems(NbtMapBuilder tag) {
        tag.putList("Items", NbtType.END, List.of());
        tag.remove("IsSplash");
    }

    @Override
    public CauldronContents getContents() {
        return data().getContents();
    }

    @Override
    public void setContents(CauldronContents contents) {
        Objects.requireNonNull(contents, "contents");

        if (!contents.equals(getContents())) {
            data().setContents(contents);
            this.setDirty();
            this.spawnToAll();
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
