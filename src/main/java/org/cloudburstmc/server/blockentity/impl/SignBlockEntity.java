package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Sign;
import org.cloudburstmc.server.event.block.SignChangeEvent;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.Arrays;
import java.util.Objects;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class SignBlockEntity extends BaseBlockEntity implements Sign {

    private static final String[] LEGACY_TEXT_TAGS = {"Text1", "Text2", "Text3", "Text4"};

    private String[] text = {"", "", "", ""};
    private String textOwner = "";

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
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        String[] text;
        if (!tag.containsKey("Text")) {
            text = new String[4];
            for (int i = 0; i < 4; i++) {
                text[i] = tag.getString(LEGACY_TEXT_TAGS[i], "");
            }
        } else {
            text = Arrays.copyOf(tag.getString("Text").split("\n", 4), 4);
        }

        this.text = text;

        tag.listenForString("TextOwner", this::setTextOwner);
    }

    @Override
    public void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);

        tag.putString("Text", String.join("\n", this.text));
        tag.putString("TextOwner", this.textOwner);
    }

    @Override
    public boolean isValid() {
        Identifier blockId = getBlockState().getType();
        return blockId == BlockIds.OAK_STANDING_SIGN || blockId == BlockIds.OAK_WALL_SIGN ||
                blockId == BlockIds.SPRUCE_STANDING_SIGN || blockId == BlockIds.SPRUCE_WALL_SIGN ||
                blockId == BlockIds.BIRCH_STANDING_SIGN || blockId == BlockIds.BIRCH_WALL_SIGN ||
                blockId == BlockIds.JUNGLE_STANDING_SIGN || blockId == BlockIds.JUNGLE_WALL_SIGN ||
                blockId == BlockIds.ACACIA_STANDING_SIGN || blockId == BlockIds.ACACIA_WALL_SIGN ||
                blockId == BlockIds.DARK_OAK_STANDING_SIGN || blockId == BlockIds.DARK_OAK_WALL_SIGN;
    }

    public void setText(String... lines) {
        for (int i = 0; i < 4; i++) {
            if (i < lines.length)
                text[i] = lines[i];
            else
                text[i] = "";
        }

        this.spawnToAll();
        this.setDirty();
    }

    public String[] getText() {
        return Arrays.copyOf(text, text.length);
    }

    public String getTextOwner() {
        return textOwner;
    }

    public void setTextOwner(String textOwner) {
        this.textOwner = textOwner;
    }

    @Override
    public boolean updateNbtMap(NbtMap tag, Player player) {
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
                text[i] = TextFormat.clean(text[i]);
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
