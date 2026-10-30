package org.cloudburstmc.server.blockentity;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Sign;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.SignData;
import org.cloudburstmc.api.event.block.SignChangeEvent;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.adventure.BedrockLegacyTextSerializer;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Arrays;
import java.util.Objects;

public class SignBlockEntity extends BaseBlockEntity implements Sign {

    private static final String[] LEGACY_TEXT_TAGS = {"Text1", "Text2", "Text3", "Text4"};

    public SignBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    private static void sanitizeText(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            // Don't allow excessive characters per line.
            if (lines[i] != null) {
                lines[i] = lines[i].substring(0, Math.min(255, lines[i].length()));
            }
        }
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return SignData.class;
    }

    @Override
    protected SignData data() {
        return (SignData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (!tag.containsKey("Text")) {
            String[] text = new String[4];
            for (int i = 0; i < 4; i++) {
                text[i] = tag.getString(LEGACY_TEXT_TAGS[i], "");
            }
            data().setText(String.join("\n", text));
        }
    }

    public String[] getText() {
        String text = data().getText();
        return Arrays.copyOf(text != null ? text.split("\n", 4) : new String[0], 4);
    }

    public void setText(String... lines) {
        String[] text = new String[4];
        for (int i = 0; i < 4; i++) {
            if (i < lines.length)
                text[i] = lines[i];
            else
                text[i] = "";
        }

        data().setText(String.join("\n", text));
        this.spawnToAll();
        this.setDirty();
    }

    public String getTextOwner() {
        String textOwner = data().getTextOwner();
        return textOwner != null ? textOwner : "";
    }

    public void setTextOwner(String textOwner) {
        data().setTextOwner(textOwner);
    }

    @Override
    public boolean updateNbtMap(NbtMap tag, CloudPlayer player) {
        String[] splitText = tag.getString("Text").split("\n", 4);
        String[] text = new String[4];

        for (int i = 0; i < 4; i++) {
            if (i < splitText.length)
                text[i] = splitText[i];
            else
                text[i] = "";
        }

        sanitizeText(text);

        SignChangeEvent event = new SignChangeEvent(this.getBlock(), player, text);

        if (!tag.containsKey("TextOwner") || !Objects.equals(player.getXuid(), tag.getString("TextOwner"))) {
            event.setCancelled();
        }

        if (player.getRemoveFormat()) {
            for (int i = 0; i < 4; i++) {
                text[i] = PlainTextComponentSerializer.plainText().serialize(
                        BedrockLegacyTextSerializer.getInstance().deserialize(text[i]));
            }
        }

        this.server.getEventManager().fire(event);

        if (!event.isCancelled()) {
            this.setText(event.getLines());
            return true;
        }

        return false;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
