package org.cloudburstmc.server.player;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.player.skin.Skin;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.nbt.serializer.SkinNbt;

import java.util.List;
import java.util.OptionalLong;
import java.util.Set;

public class PlayerSaveData {

    private static final String INVENTORY = "Inventory";
    private static final String ENDER_CHEST = "EnderChestInventory";
    private static final String BASE_MOVEMENT_SPEED = "Cloudburst.BaseMovementSpeed";

    private static final Set<String> KEYS = Set.of("firstPlayed", "lastPlayed", "Level", "playerGameType",
            "SpawnLevel", "SpawnX", "SpawnY", "SpawnZ", "SpawnYaw", "SpawnType",
            "foodLevel", "foodSaturationLevel", "foodExhaustionLevel", "foodTickTimer", "Skin",
            INVENTORY, ENDER_CHEST, "EXP", "expLevel", "enchantmentSeed", "seenCredits", BASE_MOVEMENT_SPEED
    );

    private @Nullable Long firstPlayed;
    private @Nullable Long lastPlayed;
    private @Nullable String levelId;
    private @Nullable GameMode gameMode;
    private Respawn respawn = Respawn.EMPTY;

    public static Set<String> ownedKeys() {
        return KEYS;
    }

    public OptionalLong firstPlayed() {
        return this.firstPlayed == null ? OptionalLong.empty() : OptionalLong.of(this.firstPlayed);
    }

    public OptionalLong lastPlayed() {
        return this.lastPlayed == null ? OptionalLong.empty() : OptionalLong.of(this.lastPlayed);
    }

    public @Nullable String levelId() {
        return this.levelId;
    }

    public void setLevelId(@Nullable String levelId) {
        this.levelId = levelId;
    }

    public @Nullable GameMode gameMode() {
        return this.gameMode;
    }

    public void setGameMode(@Nullable GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public Respawn respawn() {
        return this.respawn;
    }

    public void setRespawn(String levelId, Vector3i position, float yaw, RespawnConfig.@Nullable SpawnType type) {
        this.respawn = new Respawn(levelId, position.getX(), position.getY(), position.getZ(), yaw, type);
    }

    public void clearRespawn() {
        this.respawn = Respawn.EMPTY;
    }

    public Food load(NbtMap tag) {
        this.firstPlayed = tag.get("firstPlayed") instanceof Number value ? value.longValue() : null;
        this.lastPlayed = tag.get("lastPlayed") instanceof Number value ? value.longValue() : null;
        this.levelId = tag.get("Level") instanceof String id ? id : null;
        if (tag.get("playerGameType") instanceof Number value) {
            this.gameMode = GameMode.from(value.intValue());
        }
        this.respawn = Respawn.read(tag);
        return new Food(intOr(tag, "foodLevel", 20), floatOr(tag, "foodSaturationLevel", 5f),
                floatOr(tag, "foodExhaustionLevel", 0f), intOr(tag, "foodTickTimer", 0));
    }

    public void save(NbtMapBuilder tag, Food food, @Nullable Skin skin) {
        if (this.firstPlayed != null) {
            tag.putLong("firstPlayed", this.firstPlayed);
        }

        if (this.lastPlayed != null) {
            tag.putLong("lastPlayed", this.lastPlayed);
        }

        if (this.levelId != null) {
            tag.putString("Level", this.levelId);
        }

        if (this.gameMode != null) {
            tag.putInt("playerGameType", this.gameMode.getVanillaId());
        }

        this.respawn.write(tag);

        tag.putInt("foodLevel", food.level());
        tag.putFloat("foodSaturationLevel", food.saturation());
        tag.putFloat("foodExhaustionLevel", food.exhaustion());
        tag.putInt("foodTickTimer", food.tickTimer());
        if (skin != null) {
            tag.putCompound("Skin", SkinNbt.toNbt(skin));
        }
    }

    public static void loadInventories(NbtMap tag, CloudContainer inventory, CloudContainer enderChest) {
        readInventory(tag, INVENTORY, inventory);
        readInventory(tag, ENDER_CHEST, enderChest);
    }

    public static void saveInventories(NbtMapBuilder tag, CloudContainer inventory, CloudContainer enderChest) {
        tag.putList(INVENTORY, NbtType.COMPOUND, inventory.toNbt());
        tag.putList(ENDER_CHEST, NbtType.COMPOUND, enderChest.toNbt());
    }

    private static void readInventory(NbtMap tag, String key, CloudContainer container) {
        List<NbtMap> entries = tag.getList(key, NbtType.COMPOUND);
        if (entries.isEmpty()) {
            return;
        }

        ItemStack[] items = new ItemStack[container.size()];
        for (NbtMap entry : entries) {
            int slot = entry.getByte("Slot");
            if (slot < 0 || slot >= items.length) {
                throw new IndexOutOfBoundsException(key + " slot " + slot + " is outside a container of size " + items.length);
            }
            items[slot] = ItemStackNbtCodec.deserializeItem(entry);
        }

        for (int slot = 0; slot < items.length; slot++) {
            if (items[slot] != null) {
                container.setItem(slot, items[slot]);
            }
        }
    }

    private static int intOr(NbtMap tag, String key, int fallback) {
        return tag.get(key) instanceof Number value ? value.intValue() : fallback;
    }

    private static float floatOr(NbtMap tag, String key, float fallback) {
        return tag.get(key) instanceof Number value ? value.floatValue() : fallback;
    }

    private static @Nullable Integer coordinate(NbtMap tag, String key) {
        return tag.get(key) instanceof Number value ? value.intValue() : null;
    }

    public record Food(int level, float saturation, float exhaustion, int tickTimer) {
    }

    public record Progress(int experience, int experienceLevel, int enchantmentSeed, boolean seenCredits, float baseMovementSpeed) {

        public static Progress read(NbtMap tag, int defaultEnchantmentSeed, float defaultMovementSpeed) {
            float speed = floatOr(tag, BASE_MOVEMENT_SPEED, defaultMovementSpeed);
            if (!Float.isFinite(speed) || speed < 0f) {
                speed = defaultMovementSpeed;
            }
            return new Progress(intOr(tag, "EXP", 0), intOr(tag, "expLevel", 0),
                    intOr(tag, "enchantmentSeed", defaultEnchantmentSeed), tag.getBoolean("seenCredits"), speed);
        }

        public void write(NbtMapBuilder tag, float defaultMovementSpeed) {
            tag.putInt("EXP", this.experience);
            tag.putInt("expLevel", this.experienceLevel);
            tag.putInt("enchantmentSeed", this.enchantmentSeed);
            tag.putBoolean("seenCredits", this.seenCredits);
            if (this.baseMovementSpeed != defaultMovementSpeed) {
                tag.putFloat(BASE_MOVEMENT_SPEED, this.baseMovementSpeed);
            }
        }
    }

    public record Respawn(@Nullable String levelId, @Nullable Integer x, @Nullable Integer y, @Nullable Integer z, @Nullable Float savedYaw, RespawnConfig.@Nullable SpawnType type) {

        private static final Respawn EMPTY = new Respawn(null, null, null, null, null, null);

        public @Nullable Vector3i position() {
            return this.x != null && this.y != null && this.z != null ? Vector3i.from(this.x, this.y, this.z) : null;
        }

        public float yaw() {
            return this.savedYaw != null ? this.savedYaw : 0f;
        }

        private static Respawn read(NbtMap tag) {
            RespawnConfig.SpawnType type = switch (tag.get("SpawnType")) {
                case String name when name.equals("BED") -> RespawnConfig.SpawnType.BED;
                case String name when name.equals("RESPAWN_ANCHOR") -> RespawnConfig.SpawnType.RESPAWN_ANCHOR;
                case String name when name.equals("FORCED") -> RespawnConfig.SpawnType.FORCED;
                case null, default -> null;
            };

            return new Respawn(tag.get("SpawnLevel") instanceof String id ? id : null,
                    coordinate(tag, "SpawnX"), coordinate(tag, "SpawnY"), coordinate(tag, "SpawnZ"),
                    tag.get("SpawnYaw") instanceof Number value ? value.floatValue() : null, type);
        }

        private void write(NbtMapBuilder tag) {
            if (this.levelId != null) {
                tag.putString("SpawnLevel", this.levelId);
            }

            if (this.x != null) {
                tag.putInt("SpawnX", this.x);
            }

            if (this.y != null) {
                tag.putInt("SpawnY", this.y);
            }

            if (this.z != null) {
                tag.putInt("SpawnZ", this.z);
            }

            if (this.savedYaw != null) {
                tag.putFloat("SpawnYaw", this.savedYaw);
            }

            if (this.type != null) {
                tag.putString("SpawnType", this.type.name());
            }
        }
    }
}
