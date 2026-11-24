package org.cloudburstmc.server.entity.hostile;

import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.boss.BossBar;
import org.cloudburstmc.api.boss.BossBarColor;
import org.cloudburstmc.api.boss.BossBarStyle;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.hostile.Wither;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.testutil.PersistenceFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WitherBossBarTest {

    @Test
    void eachWitherOwnsItsBossBar() {
        Wither wither = PersistenceFixtures.entity(EntityTypes.WITHER);
        BossBar bar = wither.getBossBar();

        assertSame(bar, wither.getBossBar());
        assertNotSame(bar, PersistenceFixtures.entity(EntityTypes.WITHER).getBossBar());
        assertEquals(Component.translatable("entity.wither.name"), bar.getTitle());
        assertEquals(BossBarColor.PURPLE, bar.getColor());
        assertEquals(BossBarStyle.SOLID, bar.getStyle());
    }

    @Test
    void progressFollowsHealthAndMaximumHealth() {
        Wither wither = PersistenceFixtures.entity(EntityTypes.WITHER);
        BossBar bar = wither.getBossBar();

        wither.setHealth(150);
        assertEquals(0.5, bar.getProgress());

        wither.setMaxHealth(600);
        assertEquals(0.25, bar.getProgress());
    }

    @Test
    void loadingHealthAndNameUpdatesTheExistingBar() {
        EntityWither wither = (EntityWither) PersistenceFixtures.entity(EntityTypes.WITHER);
        BossBar bar = wither.getBossBar();

        wither.loadAdditionalData(NbtMap.builder()
                .putFloat("Health", 75f)
                .putString("CustomName", "Wither of the North")
                .build());

        assertSame(bar, wither.getBossBar());
        assertEquals(0.25, bar.getProgress());
        assertEquals(Component.text("Wither of the North"), bar.getTitle());

        wither.customName(null);
        assertEquals(Component.translatable("entity.wither.name"), bar.getTitle());
    }
}
